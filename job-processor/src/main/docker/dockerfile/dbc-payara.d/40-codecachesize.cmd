create-jvm-options '-XX\:ReservedCodeCacheSize=2G' --passwordfile=./passfile.txt
create-jvm-options '-XX\:InitialCodeCacheSize=100M' --passwordfile=./passfile.txt
create-jvm-options '-XX\:CodeCacheExpansionSize=1M' --passwordfile=./passfile.txt
create-jvm-options '-XX\:MinMetaspaceExpansion=1M' --passwordfile=./passfile.txt
create-jvm-options '-XX\:-BackgroundCompilation' --passwordfile=./passfile.txt
create-jvm-options '-XX\:+TieredCompilation' --passwordfile=./passfile.txt
