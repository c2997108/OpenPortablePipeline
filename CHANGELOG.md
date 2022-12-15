
# 1.1.3 (10/19/2022)
- Add ```metagenome~PR2_NCBI-16S-mito-plastid_single-end```.

# 1.1.2 (10/13/2022)
- Fixed an error ```broken pipe``` when using pipes with docker. Scope of fixes: ```annotation~Trinotate, preprocessing~exclude-specific-entries-in-FASTA```

# 1.1.1 (9/29/2022)
- Add ```metagenome~PR2_NCBI-16S-mito-plastid_paired-end, metagenome~mapping-to-MAG, metagenome~mapping-to-MAG-with-full-assembly, preprocessing~download-SRA-FASTQ```.
- Bugfix for ```RNA-seq~Trinity-kallisto-sleuth, statistics~all-sample-combinations-DESeq2-edgeR```.

# 1.1.0 (7/25/2022)
- Fixed a bug that docker does not start on WSL Ubuntu 22
- Add "salmon, bbmap, metagenome~mapping-to-mag"
- Fixed error when moving folders when using singularity.
- Added ability to run chroot in Docker(c2997108/ubuntu:20.04-singularity_pp) for Terra (but it does not bind /proc etc., so java etc. does not work)

# 1.0.9 (5/12/2022)
- When using WSL and logged in as a user without administrator privileges, wsl --install does not install to the administrator's account, so added a method to set it up with wsl --import
- Fixed a bug in linkage-analysis~single-cell_CellRanger-VarTrix script that the output delimiter of awk was not reflected in some environments.
- Fixed an error in Comparative-genomics~circos-plot-with-single-copy-gene when the FASTA sequence name contains ";" or "=".

# 1.0.8 (5/9/2022)
- Fixed a bug that the options starting with -e, -E, -n were being erased
- added comparative-genomics~circos-plot-with-single-copy-gene
- Fixed a bug in the post-assemble~dotplot-by-minimap2 script

# 1.0.7 (4/30/2022)
- Fixed a bug in the post-assemble~dotplot-by-minimap2 script where samtools was not being called via container
- Supported analysis when using WSL on Windows 11; changed to support only when using WSL2.

# ver1.0.4 (3/31/2022)
- Changed to mount the original path with docker/singularity when the input file is a symbolic link. singularity must have a folder under /home to start `scripts' when tested before, or a registered container must contain a /mnt folder must be included. Otherwise, it would not be possible to use --pwd to move to that folder, resulting in an error. I tried with the current version and it was fine, so I decided to stop binding to /mnt and bind directly to the current absolute path.

# ver1.0.3 (3/30/2022)
- Changed python in pp.py from 2->3, since there is no longer a python command on CentOS8 and Ubuntu20.

# ver1.0.1 (1/21/2022)
- DO_, $ENV_ is now used not only to start docker and singularity, but also to store container IDs, etc., so using DO_, $ENV_ after "|" was not supported.

# ver1.0 (1/17/2022)
- Changed so that each script is invoked from the python wrapper. This allows to stop a Docker container in the middle of execution, and to stop a running analysis tool when canceling from Portable Pipeline.
- Fixed a bug in post-assemble~dotplot-by-minimap2 that occasionally caused it to not target across diagonals, due to minimap2's failure to grasp the specification that results are different when query and DB are reversed. Fixed by swapping the query and DB and running it twice.

# 0.9e (1/16/2020)
- Support passphrase for ssh private key
- Boundary size of job and script selection screens can be changed
