"""Download the background photos listed in assets/photos.json into assets/photos/ (run by the build)."""
import json, os, sys, time, urllib.request

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets")
LIST = os.path.join(ROOT, "photos.json")
OUT = os.path.join(ROOT, "photos")
WIDTH = 1920  # the TV draws apps at 1080p; 1920 px wide is full sharpness

os.makedirs(OUT, exist_ok=True)
urls = [p["url"] for p in json.load(open(LIST, encoding="utf-8"))["photos"]]
ok, failed = 0, []
for i, url in enumerate(urls, 1):
    src = url
    if "images.unsplash.com" in url:
        src = url.split("?")[0] + f"?w={WIDTH}&q=82&fm=jpg"
    dest = os.path.join(OUT, f"photo_{i:02d}.jpg")
    for attempt in range(3):
        try:
            req = urllib.request.Request(src, headers={"User-Agent": "Athkari-build"})
            data = urllib.request.urlopen(req, timeout=60).read()
            if not data.startswith(b"\xff\xd8"):
                raise ValueError("not a JPEG")
            open(dest, "wb").write(data)
            ok += 1
            print(f"OK   {i:02d}  {len(data)//1024:5d} KB  {url}")
            break
        except Exception as e:
            if attempt == 2:
                failed.append(url)
                print(f"FAIL {i:02d}  {url}  ({e})")
            time.sleep(3)

print(f"\n{ok} of {len(urls)} photos bundled.")
if failed:
    print("Could not download:\n  " + "\n  ".join(failed))
if ok < max(1, int(len(urls) * 0.8)):
    sys.exit("Too many photos failed to download; stopping the build.")
