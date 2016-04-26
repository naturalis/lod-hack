# Collectors

Builds an RDF graph of specimens held by The Natural History Museum, Naturalis and Botanischer Garten & Botanisches Museum, and collected by the same person. 

These collectors nodes are defined by http://viaf.org/ identifiers. The following collectors are used:

* Wallich, Nathaniel, 1786-1854 (present at B (176 counts)) http://viaf.org/viaf/9962177

* Adolf Engler (present at B (200 counts)) http://viaf.org/viaf/73914023

* Sellow, Friedrich 1789-1831 (present at B (1159 counts)) http://viaf.org/viaf/88234994

* Humboldt, Alexander von (Friedrich Wilhelm Heinrich Alexander), 1769-1859 (present at B (2500 counts)) http://viaf.org/viaf/95193235

* Bonpland, Aimé Jacq.Alex 1773-1858 (present at B (2500 counts)) http://viaf.org/viaf/51750221

RUN
---

python collectors.py

Searches NHM, Naturalis and BGBM Data endpoints to find all specimen records collected.

Builds RDF turtle graph, output as collections.ttl

