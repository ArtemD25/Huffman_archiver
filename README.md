# Huffman_archiver

The task was to write a program that can archive as well as unarchive files (no matter of their size) using Huffman algorithm. See more details here: https://en.wikipedia.org/wiki/Huffman_coding

If you want to archive a file you shall use command line arguments this way:
- file.txt - this will archive file.txt to file.txt.par archive. "Par" is zip-like extension saying that smth was archived.
- file.txt file.txt.par - this way you will explicitly say you want to get the file.txt.par file in result
- file.txt no_file.txt.par - feel free to change the name of the archived file
- -a file.txt file.txt.par - the "-a" flag will force the program to archive the first fole to the second one

Unarchiving works the same way:
- file.txt.par - the program will unarchive it to the file.txt
- file.txt.par file.txt - no comment needed
- file.par file.uar - "uar" stands for "unknown archive" meaning that we do not know anymore the initial file extension. That is why the program unarchives the archive correctly but can not identify the initial extension
- -u file.txt.par file.txt - forces the program to unarchive the first file into the second one

Good luck!
