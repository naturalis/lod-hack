#!/usr/bin/perl
use strict;
use warnings;
use Getopt::Long;
use Data::Dumper;
use Bio::Phylo::IO 'unparse';
use Bio::Phylo::Util::Logger ':levels';
use Bio::Phylo::PhyloWS::Client::NBA::TaxonSearch;

# process command line arguments
my $query;
my $format;
my $verbosity = WARN;
GetOptions(
	'query=s'  => \$query,
	'format=s' => \$format,
	'verbose+' => \$verbosity,
);

# instantiate helper objects
my $log = Bio::Phylo::Util::Logger->new( '-level' => $verbosity );
my $ts  = Bio::Phylo::PhyloWS::Client::NBA::TaxonSearch->new;

# run the query
my $qr = $ts->get_query_result( -query => $query );
print $qr->to_xml;