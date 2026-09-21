#!/bin/bash

explanation='
Pipeline for adding SILVA SSU & LSU, PR2, full-length mitochondrial, full-length plastid, and MitoFish database annotations to a nanopore~get-consensus result table
'
inputdef='
input_1::a nanopore~get-consensus result table:all.cnt.seq.qual.txt
'
optiondef='
opt_c:cpu threads:8
opt_m:memory limit (GB):64
opt_t:Score from top of BLAST hits used in LCA (0-1):1
opt_i:Internal control sequence:TCACCAACTGGGATGACATGGAGAAGATCTGGCACCACACCTTCTACAATGAGCTGCATGTGGCTCCCAAGGAGCACCGTATGCTGCTGACTGAGGTCCCCCTGAATCCAAGGCCAACCACAAGAAGATGA
'
runcmd="$0 -c #opt_c# -m #opt_m# -t #opt_t# #input_1#"

export IM_SEQKIT="c2997108/ppmultiarch:vsearch_mafft_muscle_emboss_exonerate_6"
export IM_BLAST="c2997108/ppmultiarch:blast_seqkit_1"
export IM_EXCEL="c2997108/ppmultiarch:yoshitake_merge"

source "$(dirname "$(readlink -f "$0" 2>/dev/null || echo "$0")")/common.sh"

set -eux
set -o pipefail

mkdir -p temp-fasta
cut -f 1,2 "$input_1"|tail -n+2| DO_SEQKIT seqkit tab2fx > temp-fasta/all.cnt.seq.qual.txt.fa
bash "$scriptdir"/metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_single-end -c $opt_c -m $opt_m -s 100 -t $opt_t temp-fasta
cat split/*/*.lca > temp-fasta.lca
echo ">internalcontrol
$opt_i" > internalcontrol.fa

DO_BLAST makeblastdb -in internalcontrol.fa -dbtype nucl
DO_BLAST blastn -outfmt 6 -num_threads $N_CPU -db internalcontrol.fa -query temp-fasta/all.cnt.seq.qual.txt.fa > temp-fasta.control
(cat temp-fasta.lca; awk -F'\t' '$3>=80&&$4>=60{OFS="\t"; $1="internal_control\tinternal_control\t"$1; print $0}' temp-fasta.control) > temp-fasta.lca.plus.control

awk -F'\t' 'FILENAME==ARGV[1]{lca[$3]=$1; topname[$3]=$2; len[$3]=$6; ident[$3]=$5} FILENAME==ARGV[2]{if(FNR==1){OFS="\t"; $3=$3"\tlca\ttop.taxpath\talign.len\tidentity"}else{$3=$3"\t"lca[$1]"\t"topname[$1]"\t"len[$1]"\t"ident[$1]}; print $0}' temp-fasta.lca.plus.control "$input_1" > all.cnt.seq.qual.tax.txt
awk -F'\t' 'FILENAME==ARGV[1]{if(FNR==1){OFS="\t"; for(i=1;i<=NF;i++){h[i]=$i}}else{tmp=$4":"$5; if(!(tmp in cnt)){for(j=1;j<=7;j++){data2[tmp][j]=$j}}; for(i=8;i<=NF;i++){data[tmp][i]+=$i; cnt[tmp]+=$i}}} END{ORS=""; print "id"; for(i=2;i<=NF;i++){print "\t"h[i]}; print "\n"; PROCINFO["sorted_in"]="@val_num_desc"; for(j in cnt){print data2[j][1]; for(k=2;k<=7;k++){print "\t"data2[j][k]}; for(k=8;k<=NF;k++){print "\t"data[j][k]}; print "\n"}}' all.cnt.seq.qual.tax.txt > all.cnt.seq.qual.tax.sp.txt
DO_EXCEL java -jar /usr/local/bin/excel2.jar all.cnt.seq.qual.tax.sp.txt all.cnt.seq.qual.tax.sp.xlsx

post_processing

#<option detail>
#</option detail>

