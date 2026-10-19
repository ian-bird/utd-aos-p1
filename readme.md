# Steps to run

1. make sure jdk 22 javac and java are on the path (`cd ~` `source ./.bashrc` `cd -`)
2. Build the project by navigating to `project/p1`. run `javac src/main/java/utd/aos/p1/*.java src/main/java/utd/aos/p1/**/*java -d target`
3. navigate to the target directory. (`cd target`)
4. Copy the resources over. `cp ../src/main/resources/**.txt .`
5. Run with `/usr/local/jdk-22.0.1/bin/java utd/aos/p1/Main`

Note: all processes must be started within 2 seconds.
