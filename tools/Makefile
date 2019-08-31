# NOTE: This Makefile is not required to build the program, for which maven
# is used. Instead, it invokes the program for tests and for transforming the
# output, for example to the lirc.xml file.

BROWSE := xdg-open

MYDIR := $(dir $(firstword $(MAKEFILE_LIST)))
TOP := $(realpath $(MYDIR)..)

include $(MYDIR)/paths.mk

GH_PAGES := $(TOP)/gh-pages
ORIGINURL := $(shell git remote get-url origin)
EXTRACT_VERSION := $(TOP)/tools/extract_project_version.xsl
VERSION := $(shell $(XSLTPROC) $(EXTRACT_VERSION) pom.xml)

default:
	mvn install

apidoc: target/site/apidocs
	$(BROWSE) $</index.html

javadoc: target/site/apidocs

target/site/apidocs:
	mvn javadoc:javadoc

gh-pages: target/site/apidocs
	rm -rf $(GH_PAGES)
	git clone --depth 1 -b gh-pages ${ORIGINURL} ${GH_PAGES}
	( cd ${GH_PAGES};  \
	cp -r ../target/site/apidocs/* . ; \
	git add * ; \
	git commit -a -m "Update of API documentation" ; \
	git push )

tag:
	git checkout master
	git status
	git tag -a Version-$(VERSION) -m "Tagging Version-$(VERSION)"
	git push origin Version-$(VERSION)

clean:
	mvn clean
	rm -rf $(GH_PAGES)

.PHONY: clean
