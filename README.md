# Stream Sampler

[![Build Status](https://travis-ci.org/botchniaque/stream-sampler-java.svg?branch=master)](https://travis-ci.org/botchniaque/stream-sampler-java)

Simple stream sampler returning a _random representative sample_ of a specified length. 

Assumptions made while implementing the solution can be found [here](ASSUMPTIONS.md).

## Usage
```bash
mvn install
cat file.txt | ./stream-sampler -n SIZE
```

```
./stream-sampler --help
Creates a random representative sample of length SIZE out of the input.
Input is either STDIN, or randomly generated within application.
If SEED is specified, then it's used for both - sample creation and input
generation.
 -g,--generate <INPUT_SIZE>   Size of random input to generate out of
                              'abcdefghijklmnoprstuwxyz'
    --help                    Show this message
 -n,--size <SIZE>             Sample size
 -s,--seed <SEED>             Seed for random number generator
```

```bash
dd if=/dev/urandom count=100 bs=100MB | base64 | ./stream-sampler -n 100
```