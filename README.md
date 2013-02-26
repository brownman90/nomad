[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/hudvin/nomad/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

Nomad - focused highly customizable web crawler
===

Features
===

1. Crawling of multiply domains
2. Allows to write flexible rules to decide which links crawl.
3. Support of robots.txt
4. MongoDB(GridFS) as storage for crawled content
5. TitanDB(with InMemory, BerkeleyDB or Cassandra backend) to store graph of links.
6. Written in Scala.
7. Works in Linux. It should work in Win as well, but I haven't tested it.

How to get
===

### From source
Nomad uses gradle as build system. To build from source you need:

1. Install gradle
2. Checkout src
3. Go to folder with builld.gradle and run

    gradle distZip
    
You can find nomad*.zip in 

     build/distributions/
     

### Binary
Download ready to use binary here https://bitbucket.org/hudvin/nomad/downloads/nomad-release-0.3.zip


How to run 
====


### Prerequisite

1. JRE/JDK 7
2. MongoDB
3. Linux. Currently tested on Debian 7 only.


To run nomad you need execute from shell:

    ./bin/nomad <path to profile>
    
for example:
      ./bin/nomad profiles/template
      

###What is profile?

To simplify usage of different congigurations nomad allows to create profiles. Profile is a folder with 3 files:

1. application.conf. Contains configuration of graph and files storages and configuration of crawling strategy.
2. filters.groovy. Groovy file with two functions - filterUrl and filterEntity. Here you can define any logic you want to filter urls and files.
3. seed.txt - list of urls to crawl.
      
####application.conf

        app {
            //name of file with urls
            default_seed = seed.txt
        } 
        master {
            //one worker crawles one domain, so number of workers mean number of simultaneously crawled domains
            workers = 10
            //number of links fetched simultaneously
            threads_in_worker = 10
        }
        links{
            //size of cache for links to crawl
            bfs_limit = 5000
            //links extracted from pages are stored in memory, when number of links becomes larger than this value
            //they are flushed to db
            extracted_links_cache = 200000
        }       
        storage{
            //mongo is used as storage for all fetched files
            mongo{
                host = "127.0.0.1"
                port = 27017
                db_name = nomad
                drop = true
            }
            //titan and blueprints are used as storage for graph of links
            titan{
                //backed for titan - inmemory, cassandra or berkeley
                //drop=true means that db will be dropped on each start
                main_connector = inmemory
                backends{
                cassandra{
                    host = "127.0.0.1"
                    drop = false
                }
                berkeley{
                    directory = /tmp/berkeley
                    drop = true
                }
                inmemory{
                }
            }
        }
    }
        

####filters.groovy
Contains two functions
```groovy
def filterUrl(url) {return true}

def filterEntity(size, url, mimeType) {return truef}
```

If function returns true, url or file(entity) will be downloaded, otherwise - skipped. 
filterUrl is called after link has been extracted.
So if filterUrl returns false for this link, nomad will never try to crawle it. 
filterEntity is called after headers for file is received. 
If funtions returns false then file is skipped. 
It may be useful to prevent downloading of large files, for example.


Example of implementation(from profiles/template/filters.groovy):

```groovy
def filterUrl(url) {
    if (url.contains(".pdf")) {
         return false
    }
    if (url.contains(".tgz")) {
        return false
    }
    if (url.contains("http://consc.net/online/")) {
        return false
    }
    return true
}

def filterEntity(size, url, mimeType) {
    if (size > 10000000) {
        return false
    }
    return true

}

```
####seed.txt
Contains list of urls to crawl. Each url must looks like 

    http(s)://ibm.com
    

Notes
===

1. It's still contains a lot of bugs.
2. I am working on external API to provide access to graph and files.
3. Need to check stability.
4. Need to perform optimization.
    
