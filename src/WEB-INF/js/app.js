/*
 @author Scott Faria
 */
$(document).ready(function () {
    const searchBox = $("#search_text");
    searchBox.focus();
    searchBox.keyup(function(event) {
        if (event.keyCode == 13){
            $("#submit_search").click();
        }
    });
});

function performSearch() {
    const searchText = $('#search_text').val();
    if (searchText) {
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
                    const results = response.results;
                    var responseHtml = `Found the following ${results.length} quotes in ${response.elapsed_time}ms:`;
                    results.forEach((element, index) => {
                        responseHtml += '<div class=\"search_hit_set\">\n';
                        responseHtml += `<h2>${element.title} by ${element.author}</h2>`;
                        responseHtml += `"...${element.context}..."`;
                        responseHtml += '</div>\n';
                    });
                    resultsDiv.append(responseHtml);
                }
            },
            error: function() {
                $('#search_results').empty().append('Something went way wrong.');
            }
        });
    }
}

function uploadBook() {
    const selectedFileInput = $("#selected_file");
    selectedFileInput.click();
    selectedFileInput.change(() => {
        const book = selectedFileInput.prop('files')[0];
        const formData = new FormData();
        formData.append('content', book);
        $.ajax({
            url: '/upload',
            type: 'POST',
            processData: false,
            data: formData,
            success: function(response) {
                $('#search_results').empty().append('Thank you for the upload. The book will be searchable shortly.');
            },
            error: function() {
                $('#search_results').empty().append(`Failed to upload \"${book.name}\".`);
            }
        })
    });
}
