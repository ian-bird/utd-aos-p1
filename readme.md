# Steps to run

1. make sure jdk 22 javac and java are on the path (`cd ~` `source ./.bashrc` `cd -`)
2. Build the project by navigating to `project/p1`. run `javac src/main/java/utd/aos/p1/*.java src/main/java/utd/aos/p1/**/*java -d target`
3. navigate to the target directory. (`cd target`)
4. Copy the resources over. `cp ../src/main/resources/**.txt .`
5. move back to the project root. `cd ../..`
6. if necessary, run `./cleanup.sh` to remove processes on other nodes
7. run with `./startup.sh`. The result is in snapshots.txt


