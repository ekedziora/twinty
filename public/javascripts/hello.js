var tweets = [];
var pagination;
var paginationInitiated = false;

$(function() {
    var feed;
    var searchButton = $("#searchButton");
    var closeButton = $("#closeButton");
    var messageHolder = $("#messageHolder");
    var template = _.template(
        `<div class="row">
            <div class="col-md-offset-2 col-md-8 tweet <%= sentimentClass %>">
                <div class="row">
                    <div class="col-md-12"><b>User name:</b> <%= userName %></div>
                 </div>
                 <div class="row">
                    <div class="col-md-12"><%= text %></div>
                 </div>
                 <div class="row">
                    <div class="col-md-12"><b>Sentiment:</b> <span class="sentiment <%= sentimentClass %>"><%= sentiment %></span></div>
                 </div>
            </div>
        </div>`
    );

    searchButton.click(function() {
        feed = new EventSource(feedUrl);
        feed.onmessage = function (event) {
            tweet = JSON.parse(event.data);
            tweets.push(tweet);
            var pageNumber = 1;
            if (paginationInitiated) {
                pageNumber = pagination.pagination('getSelectedPageNum');
                pagination.pagination('destroy');
            }
            pagination = $("#paginationHolder").pagination({
                dataSource: tweets,
                // hideWhenLessThanOnePage: true,
                pageSize: 5,
                className: 'paginationjs-theme-blue',
                callback: function(data, pagination) {
                    messageHolder.html("");
                    data.forEach(function(element) {
                        messageHolder.append(template(element));
                    });
                }
            });
            pagination.pagination(pageNumber);
            paginationInitiated = true;
        };

        feed.onerror = function () {
            console.log("ERROR");
        };

        setButtonsVisablity(feed);
    });

    closeButton.click(function() {
        feed.close();
        setButtonsVisablity(feed);
    });

    $(window).on('onbeforeunload', function() {
        feed.close();
    });

    setButtonsVisablity(feed);
});

function setButtonsVisablity(feed) {
    if (isDefined(feed) && (feed.readyState == EventSource.OPEN || feed.readyState == EventSource.CONNECTING)) {
        searchButton.style.display = 'none';
        closeButton.style.display = 'block';
    } else if (!isDefined(feed) || feed.readyState == EventSource.CLOSED) {
        searchButton.style.display = 'block';
        closeButton.style.display = 'none';
    }
}

function isDefined(variable) {
    return typeof variable !== "undefined";
}