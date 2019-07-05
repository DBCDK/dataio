dataIO
======

### Development

**Requirements**

To build this project JDK 1.8 and Apache Maven is required.

**Scripts**
* clean - clears build artifacts
* build - builds and tests artifacts (including docker images)
* analyse - analyses source code

```bash
./clean && ./build && ./analyse
```
To build (and test) Java artifacts only, use

 ```bash
./build nodocker
```

### License

Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3.
See license text in LICENSE.txt