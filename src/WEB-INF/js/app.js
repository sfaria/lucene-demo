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
                var responseHtml = '';
                response.forEach((element, index) => {
                    responseHtml += '<div class=\"search_hit_set\">\n    <ul>\n';
                    element.matches.forEach((element, index) => {
                        responseHtml += `   <li>${element}</li>\n`
                    });
                    responseHtml += '   </ul>\n</div>\n';
                });
                resultsDiv.append(responseHtml);
            }
        },
        error: function() {
            $('#search_results').empty().append('Something went way wrong.');
        }
    });
}
