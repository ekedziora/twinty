$(function() {
    var feed;
    var searchButton = $("#searchButton");
    var closeButton = $("#closeButton");
    var template = _.template(
        `<div class="row">
            <div class="col-md-offset-2 col-md-8 tweet">
                <div class="row">
                    <div class="col-md-12">User name: <%= userName %></div>
                 </div>
                 <div class="row">
                    <div class="col-md-12">Created: <%= creationDateTime %></div>
                 </div>
                 <div class="row">
                    <div class="col-md-12">Text: <%= text %></div>
                 </div>
                 <div class="row">
                    <div class="col-md-12">Sentiment: <%= sentiment %></div>
                 </div>
            </div>
        </div>`
    );

    searchButton.click(function() {
        feed = new EventSource(feedUrl);
        feed.onmessage = function (event) {
            tweet = JSON.parse(event.data);
            var messageHolder = $("#messageHolder");
            messageHolder.append(template(tweet));
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