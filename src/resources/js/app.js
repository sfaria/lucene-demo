/*
 @author Scott Faria
 */

function performSearch() {
    const searchText = $('#search_text').val();
    $.ajax({
        url: `/search/${encodeURIComponent(searchText)}`,
        type: 'GET',
        dataType: 'json',
        success: function(response) {
            const resultsDiv = $('#search_results');
            resultsDiv.empty();
            if (response.length == 0) {
                resultsDiv.append('No Results Found!');
            } else {
                resultsDiv.append(`Found ${response.length} results.`);
            }
        },
        error: function() {
            $('#search_results').empty().append('Something went way wrong.');
        }
    });
}
