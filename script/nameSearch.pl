#!/usr/bin/perl
use strict;
use warnings;
use Getopt::Long;
use Bio::Phylo::Util::Logger ':levels';
use Bio::Phylo::PhyloWS::Client::BGBM::NameSearch;

# process command line arguments
my $query;
my $verbosity = WARN;
GetOptions( 
	'query=s'  => \$query,
	'verbose+' => \$verbosity,
);

my $log = Bio::Phylo::Util::Logger->new( '-level' => $verbosity );

# instantiate helper objects
my $ts  = Bio::Phylo::PhyloWS::Client::BGBM::NameSearch->new;

# run the query
my $qr = $ts->get_query_result( -query => $query );
print $qr->to_xml;
