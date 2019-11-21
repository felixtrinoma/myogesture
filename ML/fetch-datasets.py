import urllib.request
import pathlib
import os

src = "http://cyril.lugan.fr/assets/stash"
dst = os.path.join("datasets", "2019-11-15-1quattro-4gestures")
files = ["2019-11-15-17-03-32.dat", "2019-11-15-17-03-32.labels.txt", "2019-11-15-17-03-32-model.specs.json"]

pathlib.Path(dst).mkdir(parents=True, exist_ok=True)

for file in files:
    url = src + "/" + file
    print("Fetching", url)
    urllib.request.urlretrieve(url, os.path.join(dst, file))