#!/usr/bin/perl
use strict;
use warnings;
use Getopt::Long;
use Bio::Phylo::PhyloWS::Client::NBA::TaxonSearch;

# process command line arguments
my $query;
GetOptions( 'query=s' => \$query );

# instantiate client object
my $ts  = Bio::Phylo::PhyloWS::Client::NBA::TaxonSearch->new;

# run the query
my $qr = $ts->get_query_result( -query => $query );

# serialize result set (for example)
print $qr->to_xml;
