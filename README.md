# flickr-exporter

## To build

Create a runnable JAR, by executing:

    sbt assembly

## Running

You will need to register for your own API key: https://www.flickr.com/services/apps/create/
When running `flickr-exporter`, specify your api key with the `-a` argument, and the secret with `-s`

### Listing photosets

    java -jar target/scala-2.11/flickr-exporter-assembly-1.0.jar list-photosets -a KEY -s SECRET -u username

### Downloading a photoset

To download a photoset, supply a photoset ID, which you can get from the previous step:

    java -jar target/scala-2.11/flickr-exporter-assembly-1.0.jar download-photoset --id PHOTOSET_ID -a KEY -s SECRET -u username

Good luck!
