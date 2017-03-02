delete-jvm-options '-Xmx512m' --passwordfile=./passfile.txt
delete-jvm-options '-Xmx4G' --passwordfile=./passfile.txt
create-jvm-options '-Xmx3G' --passwordfile=./passfile.txt
create-jvm-options '-Xms3G' --passwordfile=./passfile.txt
create-jvm-options '-XX\:ReservedCodeCacheSize=2G' --passwordfile=./passfile.txt
