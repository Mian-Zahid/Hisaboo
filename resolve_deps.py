#!/usr/bin/env python3
"""Resolve Android app dependencies from Google Maven + Maven Central using
plain urllib (JVM networking is blocked in this sandbox), following KMP
-android variants via Gradle Module Metadata (.module files).

Outputs:
  libs/*.jar          - classes.jar (and AAR libs/*.jar) for every artifact
  deps-manifest.txt   - group:artifact:version -> file mapping
"""
import json
import os
import re
import sys
import threading
import urllib.request
import urllib.error
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor
from zipfile import ZipFile

GOOGLE = "https://dl.google.com/dl/android/maven2"
CENTRAL = "https://repo.maven.apache.org/maven2"
REPOS = [GOOGLE, CENTRAL]

PROJECT = os.path.expanduser("~/workspace/kharcha-saathi")
LIBS = os.path.join(PROJECT, "libs")
AAR_CACHE = os.path.expanduser("~/workspace/downloads/aars")
os.makedirs(LIBS, exist_ok=True)
os.makedirs(AAR_CACHE, exist_ok=True)

BOM = ("androidx.compose", "compose-bom", "2024.06.00")

# Direct deps from app/build.gradle.kts (None version = from BOM).
# Room/KSP lines deliberately skipped: app uses SQLite (Data.kt), no KSP.
DIRECT = [
    ("androidx.core", "core-ktx", "1.13.1"),
    ("androidx.activity", "activity-compose", "1.9.0"),
    ("androidx.fragment", "fragment-ktx", "1.8.2"),
    ("androidx.compose.ui", "ui", None),
    ("androidx.compose.ui", "ui-graphics", None),
    ("androidx.compose.ui", "ui-tooling-preview", None),
    ("androidx.compose.material3", "material3", None),
    ("androidx.compose.material", "material-icons-extended", None),
    ("androidx.lifecycle", "lifecycle-viewmodel-compose", "2.8.2"),
    ("androidx.lifecycle", "lifecycle-runtime-compose", "2.8.2"),
    ("androidx.biometric", "biometric", "1.2.0-alpha05"),
    ("androidx.compose.ui", "ui-tooling", None),        # debugImplementation
    ("androidx.compose.ui", "ui-test-manifest", None),  # debugImplementation
    # v1.2 crash-fix extras (BUILD-README.md)
    ("androidx.graphics", "graphics-path", "1.0.0"),
    ("androidx.emoji2", "emoji2", "1.4.0"),
    ("androidx.appcompat", "appcompat", "1.7.0"),
    ("androidx.interpolator", "interpolator", "1.0.0"),
    ("androidx.customview", "customview-poolingcontainer", "1.0.0"),
    ("androidx.lifecycle", "lifecycle-livedata", "2.8.2"),
    ("androidx.lifecycle", "lifecycle-process", "2.8.2"),
]

UA = {"User-Agent": "Mozilla/5.0 (X11; Linux x86_64) resolve_deps/1.0"}
_cache = {}
_cache_lock = threading.Lock()


def fetch(url, timeout=40):
    with _cache_lock:
        if url in _cache:
            return _cache[url]
    data = None
    last = None
    for attempt in range(2):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=timeout) as r:
                data = r.read()
            break
        except Exception as e:
            last = e
    with _cache_lock:
        _cache[url] = data
    if data is None:
        print(f"  !! fetch failed: {url} ({last})", flush=True)
    return data


def repo_url(repo, g, a, v, ext):
    return f"{repo}/{g.replace('.', '/')}/{a}/{v}/{a}-{v}.{ext}"


def fetch_artifact_file(g, a, v, ext):
    """Return (bytes, repo) or (None, None)."""
    for repo in REPOS:
        data = fetch(repo_url(repo, g, a, v, ext))
        if data and not data[:6] == b"<html>":
            return data, repo
    return None, None


def strip_ns(tag):
    return tag.split("}", 1)[-1] if "}" in tag else tag


def parse_pom(xml_bytes):
    """Return dict: properties, deps[(g,a,v,scope,optional,type)], dep_mgmt{(g,a):v}, packaging."""
    root = ET.fromstring(xml_bytes)
    props, deps, mgmt = {}, [], {}
    packaging = "jar"
    for el in root:
        t = strip_ns(el.tag)
        if t == "properties":
            for p in el:
                props[strip_ns(p.tag)] = (p.text or "").strip()
        elif t == "packaging":
            packaging = (el.text or "jar").strip()
        elif t == "dependencies":
            for d in el:
                g = a = v = scope = typ = None
                optional = False
                for f in d:
                    ft = strip_ns(f.tag)
                    if ft == "groupId":
                        g = (f.text or "").strip()
                    elif ft == "artifactId":
                        a = (f.text or "").strip()
                    elif ft == "version":
                        v = (f.text or "").strip()
                    elif ft == "scope":
                        scope = (f.text or "").strip()
                    elif ft == "optional":
                        optional = (f.text or "").strip() == "true"
                    elif ft == "type":
                        typ = (f.text or "").strip()
                if g and a:
                    deps.append((g, a, v, scope, optional, typ))
        elif t == "dependencyManagement":
            for ddeps in el:
                for d in ddeps:
                    g = a = v = None
                    for f in d:
                        ft = strip_ns(f.tag)
                        if ft == "groupId":
                            g = (f.text or "").strip()
                        elif ft == "artifactId":
                            a = (f.text or "").strip()
                        elif ft == "version":
                            v = (f.text or "").strip()
                    if g and a and v:
                        mgmt[(g, a)] = v
        elif t == "parent":
            pass
    parent = None
    for el in root:
        if strip_ns(el.tag) == "parent":
            g = a = v = None
            for f in el:
                ft = strip_ns(f.tag)
                if ft == "groupId":
                    g = (f.text or "").strip()
                elif ft == "artifactId":
                    a = (f.text or "").strip()
                elif ft == "version":
                    v = (f.text or "").strip()
            parent = (g, a, v)
    return {"properties": props, "deps": deps, "mgmt": mgmt,
            "packaging": packaging, "parent": parent}


def subst(text, props):
    if not text:
        return text
    def rep(m):
        key = m.group(1)
        if key == "project.version":
            return props.get("project.version", m.group(0))
        return props.get(key, m.group(0))
    prev = None
    out = text
    for _ in range(5):
        out = re.sub(r"\$\{([^}]+)\}", rep, out)
        if out == prev:
            break
        prev = out
    return out


def get_pom_full(g, a, v):
    """Fetch POM + inherited parent chain. Returns merged dict or None."""
    pom_bytes, _ = fetch_artifact_file(g, a, v, "pom")
    if pom_bytes is None:
        return None
    merged = {"properties": {}, "deps": [], "mgmt": {}, "packaging": "jar"}
    chain = []
    cur = (g, a, v)
    seen_parents = set()
    while cur is not None:
        cg, ca, cv = cur
        if (cg, ca, cv) in seen_parents:
            break
        seen_parents.add((cg, ca, cv))
        data, _ = fetch_artifact_file(cg, ca, cv, "pom") if chain else (pom_bytes, None)
        if data is None:
            break
        parsed = parse_pom(data)
        chain.append(parsed)
        cur = parsed["parent"]
    # merge parent-first so child overrides
    for parsed in reversed(chain):
        merged["properties"].update(parsed["properties"])
        merged["mgmt"].update(parsed["mgmt"])
        if parsed["packaging"] != "jar":
            merged["packaging"] = parsed["packaging"]
    for parsed in chain:  # deps: child first, then parent (dedupe by g:a)
        for d in parsed["deps"]:
            if (d[0], d[1]) not in [(x[0], x[1]) for x in merged["deps"]]:
                merged["deps"].append(d)
    return merged


def ver_key(v):
    """Sortable key: numeric segments numerically; qualifiers dev<alpha<beta<rc<final."""
    v = v.strip()
    parts = re.split(r"[.\-]", v)
    out = []
    rank = {"dev": 0, "alpha": 1, "beta": 2, "rc": 3, "snapshot": 0}
    for p in parts:
        m = re.match(r"^(\d+)([a-zA-Z]+)?(\d*)$", p)
        if m:
            out.append((0, int(m.group(1)), ""))
            if m.group(2):
                q = m.group(2).lower()
                out.append((1, rank.get(q, 4), q))
                if m.group(3):
                    out.append((0, int(m.group(3)), ""))
        elif p.isdigit():
            out.append((0, int(p), ""))
        else:
            q = p.lower()
            out.append((1, rank.get(q, 4), q))
    # final releases sort above pre-releases of same numbers: append marker
    has_pre = any(seg[0] == 1 for seg in out)
    out.append((2, 0 if has_pre else 1, ""))
    return tuple(out)


def max_ver(a, b):
    if a is None:
        return b
    if b is None:
        return a
    return a if ver_key(a) >= ver_key(b) else b


def get_android_variant(g, a, v):
    """Check the .module file for a KMP variant redirect.
    Pass 1: androidJvm (AndroidX KMP libs -> -android module).
    Pass 2: jvm (KotlinX libs -> -jvm module, e.g. coroutines-core -> core-jvm).
    Never pick native/js/ios variants (e.g. -androidnativearm32 is a .klib)."""
    data, _ = fetch_artifact_file(g, a, v, "module")
    if not data:
        return None
    try:
        mod = json.loads(data.decode("utf-8"))
    except Exception:
        return None
    variants = mod.get("variants", [])
    for want in ("androidJvm", "jvm"):
        for variant in variants:
            avail = variant.get("available-at")
            if not avail:
                continue
            attrs = variant.get("attributes", {})
            if attrs.get("org.jetbrains.kotlin.platform.type") == want:
                return (avail.get("group", g), avail.get("module", a),
                        avail.get("version", v))
    return None


def pick_range_version(g, a, spec):
    """Resolve a Maven version range to a concrete version.
    Exact-version range [v] -> v. Bounded range [a,b) etc. -> max listed version."""
    spec = spec.strip()
    m1 = re.match(r"^\[([^\],]+)\]$", spec)
    if m1:
        return m1.group(1).strip()
    m = re.match(r"^[\[\(]([^,\]]*),([^,\]\)]*)[\]\)]$", spec)
    if not m:
        return None
    lo, hi = m.group(1).strip(), m.group(2).strip()
    lo_inc = spec.strip().startswith("[")
    hi_inc = spec.strip().endswith("]")
    versions = set()
    for repo in REPOS:
        data = fetch(f"{repo}/{g.replace('.', '/')}/{a}/maven-metadata.xml")
        if data:
            versions |= set(re.findall(r"<version>([^<]+)</version>", data.decode("utf-8", "ignore")))
    best = None
    for cand in versions:
        if lo and (ver_key(cand) < ver_key(lo) or (ver_key(cand) == ver_key(lo) and not lo_inc)):
            continue
        if hi and (ver_key(cand) > ver_key(hi) or (ver_key(cand) == ver_key(hi) and not hi_inc)):
            continue
        best = max_ver(best, cand)
    return best


def main():
    print("== loading BOM ==", flush=True)
    bom_pom = get_pom_full(*BOM)
    bom = dict(bom_pom["mgmt"]) if bom_pom else {}
    # also inherit BOM import entries that are themselves BOMs: handled via mgmt merge of pom deps? BOM pom's mgmt already lists versions.
    print(f"BOM entries: {len(bom)}", flush=True)

    resolved = {}   # (g,a) -> version (max wins)
    redirect = {}   # (g,a) -> (g2,a2,v2) android variant module
    queue = []
    queued = set()

    def enqueue(g, a, v):
        key = (g, a)
        if v is None:
            v = bom.get(key)
            if v is None:
                print(f"  !! no version for {g}:{a} (not in BOM)", flush=True)
                return
        old = resolved.get(key)
        new = max_ver(old, v)
        if old == new and key in queued:
            return
        resolved[key] = new
        if key not in queued or old != new:
            queue.append((g, a, new))
            queued.add(key)

    for g, a, v in DIRECT:
        enqueue(g, a, v)

    processed = set()
    idx = 0
    while idx < len(queue):
        g, a, v = queue[idx]
        idx += 1
        key = (g, a)
        if resolved.get(key) != v:
            continue  # superseded by a newer version
        if (g, a, v) in processed:
            continue
        processed.add((g, a, v))
        print(f"[{idx}] {g}:{a}:{v}", flush=True)
        # KMP android variant?
        red = get_android_variant(g, a, v)
        pom_sources = [(g, a, v)]
        if red:
            redirect[key] = red
            pom_sources.append(red)
            print(f"    -> android variant: {red[0]}:{red[1]}:{red[2]}", flush=True)
        for pg, pa, pv in pom_sources:
            pom = get_pom_full(pg, pa, pv)
            if pom is None:
                print(f"    !! no POM for {pg}:{pa}:{pv}", flush=True)
                continue
            props = pom["properties"]
            for (dg, da, dv, scope, optional, typ) in pom["deps"]:
                if optional:
                    continue
                if scope not in (None, "compile", "runtime"):
                    continue
                if typ not in (None, "jar", "aar"):
                    continue
                if dv is None:
                    dv = bom.get((dg, da)) or pom["mgmt"].get((dg, da))
                else:
                    dv = subst(dv, props)
                    if dv.startswith("${"):
                        dv = bom.get((dg, da)) or pom["mgmt"].get((dg, da))
                if dv is None:
                    print(f"    !! no version for dep {dg}:{da} of {pg}:{pa}", flush=True)
                    continue
                if dv.startswith("[") or dv.startswith("("):
                    dv = pick_range_version(dg, da, dv)
                    if dv is None:
                        continue
                enqueue(dg, da, dv)

    print(f"\n== resolved {len(resolved)} artifacts ==", flush=True)
    manifest = []
    lock = threading.Lock()

    def download_one(item):
        (g, a), v = item
        target = redirect.get((g, a), (g, a, v))
        tg, ta, tv = target
        # packaging from target POM
        pom = get_pom_full(tg, ta, tv)
        packaging = pom["packaging"] if pom else "jar"
        if packaging == "pom":
            with lock:
                manifest.append(f"{g}:{a}:{v} -> (pom-only, skipped)")
            return
        ext = "aar" if packaging == "aar" else "jar"
        data, repo = fetch_artifact_file(tg, ta, tv, ext)
        if data is None:
            with lock:
                print(f"  !! DOWNLOAD FAILED {tg}:{ta}:{tv}.{ext}", flush=True)
                manifest.append(f"{g}:{a}:{v} -> FAILED")
            return
        jar_name = f"{ta}-{tv}.jar"
        out_paths = []
        if ext == "aar":
            aar_path = os.path.join(AAR_CACHE, f"{ta}-{tv}.aar")
            with open(aar_path, "wb") as f:
                f.write(data)
            try:
                with ZipFile(aar_path) as z:
                    names = z.namelist()
                    if "classes.jar" in names:
                        out = os.path.join(LIBS, jar_name)
                        with open(out, "wb") as f:
                            f.write(z.read("classes.jar"))
                        out_paths.append(out)
                    for n in names:
                        if n.startswith("libs/") and n.endswith(".jar"):
                            libname = os.path.basename(n)
                            base = libname[:-4]
                            out = os.path.join(LIBS, f"{ta}-{tv}-{base}.jar")
                            with open(out, "wb") as f:
                                f.write(z.read(n))
                            out_paths.append(out)
            except Exception as e:
                print(f"  !! bad AAR {ta}-{tv}: {e}", flush=True)
        else:
            out = os.path.join(LIBS, jar_name)
            with open(out, "wb") as f:
                f.write(data)
            out_paths.append(out)
        with lock:
            for p in out_paths:
                manifest.append(f"{g}:{a}:{v} -> {os.path.basename(p)}")
            if not out_paths:
                manifest.append(f"{g}:{a}:{v} -> (no jars extracted)")

    items = sorted(resolved.items())
    with ThreadPoolExecutor(max_workers=8) as ex:
        list(ex.map(download_one, items))

    manifest.sort()
    with open(os.path.join(PROJECT, "deps-manifest.txt"), "w") as f:
        f.write("\n".join(manifest) + "\n")
    failed = [m for m in manifest if "FAILED" in m]
    print(f"\nDONE: {len(manifest)} manifest lines, jars in libs/: {len(os.listdir(LIBS))}, failures: {len(failed)}", flush=True)
    for m in failed:
        print("  " + m, flush=True)


if __name__ == "__main__":
    main()
