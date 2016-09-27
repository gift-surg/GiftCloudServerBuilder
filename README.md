GIFT-Cloud Server
-----------------

GIFT-Cloud is a secure data storage and collaboration platform for medical imaging research.

GiftCloudServerBuilder is one of the codebases required to install a GIFT-Cloud Server. It is a customised fork of [xnat_builder][xnatbuilder].

Authors: Tom Doel and Dzhoshkun Shakir, [Translational Imaging Group][tig], [Centre for Medical Image Computing][cmic], [University College London (UCL)][ucl].
GIFT-Cloud was developed as part of the [GIFT-Surg][giftsurg] project. 
If you use this software, please cite [this paper][citation]. 

GIFT-Cloud Server extends the [XNAT][xnat] system developed by Harvard University / Howard Hughes Medical Institute (HHMI) / Washington University.



Disclaimer
----------

 * GIFT-Cloud and XNAT are not certified for clinical use.


Software links
--------------

 - [GitHub mirror][githubhome].
 - [XNAT][xnat]
 - [XNAT 1.6 documentation][xnatdocumentation]
 - [XNAT 1.6 installation instructions][xnatinstall]
 - [XNAT 1.6 prerequisites][xnatprerequisites]
 - [XNAT working with modules][xnatmodule]
 - [XNAT discussion group][xnatdiscussion]
 

License
-------

Copyright (c) 2014-2016, [University College London][ucl].

GIFT-Cloud is available as free open-source software under a BSD 3-Clause License.
Parts of GIFT-Cloud derive from XNAT
 - [XNAT][xnat] (Harvard University / Howard Hughes Medical Institute (HHMI) / Washington University) uses the BSD 2-Clause License



System requirements
-------------------

GIFT-Cloud Server runs a customised version of XNAT. Please see [XNAT 1.6 prerequisites][xnatprerequisites].

GIFT-Cloud Server can be installed on Linux, macOS or Windows.

GIFT-Cloud Server requires:
 * PostgreSQL 9.1 or later
 * Oracle Java SDK 1.7 or later
 * Apache Tomcat 7.0

Depending on how you intend to deploy GIFT-Cloud Server, you may also need to configure a firewall, Apache daemon, SSL certificates, system backups etc.


Installation
------------

Installing a GIFT-Cloud Server involves two codebases:
 * GiftCloudServerBuilder is used to initially build and deploy the customised XNAT server. It is used in place of the xnat\_builder repository that would be used to build a standard XNAT installation.
 * GiftCloudServerModule is an [XNAT Module][xnatmodule] that provides required data types and additional customisations for GIFT-Cloud. This module can be built and installed after you have completed the installation with GiftCloudServerBuilder. You may wish to make further customisations to this module to configure the user interface and data types appropriate for your project.

To install Gift-Cloud Server, you will follow the XNAT installation procedure, but you will use the GiftCloudServerBuilder instead of xnat\_builder. Please procede using the following steps:
 * Install the prerequisites (Oracle Java SDK, Apache Tomcat, PostgreSQL)
 * Follow the [XNAT 1.6 Installation Guide][xnatinstall], but instead of xnat\_build, use the GiftCloudServerBuilder codebase
 * Follow the [XNAT Module documentation][xnatmodule] to install the GiftCloudServerModule



Issues
------

If you experience installation issues, they likely be related to XNAT. The best way to resolve these is through the XNAT support resources:
 * Check the [XNAT documentation][xnatdocumentation];
 * Search the [XNAT discussion group][xnatdiscussion] as your issue will often have been reported by someone else;
 * If you can't find a solution, post a new message in the [XNAT discussion group][xnatdiscussion] and explain that you are installing a customised version of XNAT 1.6.
 





Funding
-------

This work was supported through an Innovative Engineering for Health award by the [Wellcome Trust][wellcometrust] [WT101957], the [Engineering and Physical Sciences Research Council (EPSRC)][epsrc] [NS/A000027/1] and a [National Institute for Health Research][nihr] Biomedical Research Centre [UCLH][uclh]/UCL High Impact Initiative.


[tig]: http://cmictig.cs.ucl.ac.uk
[giftsurg]: http://www.gift-surg.ac.uk
[cmic]: http://cmic.cs.ucl.ac.uk
[ucl]: http://www.ucl.ac.uk

[wellcometrust]: http://www.wellcome.ac.uk
[epsrc]: http://www.epsrc.ac.uk
[nihr]: http://www.nihr.ac.uk/research
[uclh]: http://www.uclh.nhs.uk

[citation]: http://www.gift-surg.ac.uk/media-engagement/academic-journals/
[githubhome]: https://github.com/gift-surg/GiftCloudServerBuilder

[xnat]: https://www.xnat.org
[xnatinstall]: https://wiki.xnat.org/display/XNAT16/XNAT+1.6+Installation+Guide
[xnatprerequisites]: https://wiki.xnat.org/display/XNAT16/Prerequisites
[xnatbuilder]: https://bitbucket.org/nrg/xnat_builder_1_6dev
[xnatmodule]: https://wiki.xnat.org/display/XNAT16/Developing+Modules
[xnatdocumentation]: https://wiki.xnat.org/display/XNAT16/Home
[xnatdiscussion]: http://groups.google.com/group/xnat_discussion

