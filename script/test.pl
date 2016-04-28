use LWP::UserAgent;
use Data::Dumper;

# !!!! THIS IS FAILING !!!!

# This is the simplest case of running a GET request against BGBM's REST API.
# There appears to be an incompatibility between IIS 8.5 and the HTTP user agent
# implemented in the Perl library 'LWP::UserAgent'. Whoever is the culprit is
# not clear to me (@rvosa), though it does appear the the BGBM response specifies
# a content-length of 0 bytes, and the client takes this at face value (unlike
# various browsers, apparently).

my $ua = LWP::UserAgent->new;
my $response = $ua->get('http://ww2.bgbm.org/rest/herb/name/Solanum%20lycopersicum');

if ($response->is_success) {
	print "request succeeded:\n";
	print Dumper($response);
}
else {
	die $response->status_line;
}

