#!/usr/bin/perl
use strict;
use warnings;
use Data::Dumper;
use Getopt::Long;
use Bio::Phylo::PhyloWS::Client::NHM::PackageSearch;

# process command line arguments
my $query;
GetOptions( 'query=s' => \$query );

# instantiate helper objects
my $ts = Bio::Phylo::PhyloWS::Client::NHM::PackageSearch->new;

# run the query
my $qr = $ts->get_query_result( -query => $query );
print $qr->to_xml;

