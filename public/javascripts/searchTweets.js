$(function () {
    var url = jsRoutes.controllers.HomeController.searchTweetsNow();
    console.log(url);
    $("#searchButton").click(function () {
        $.ajax(url)
            .done(function (data) {
                console.log(data);
            })
            .fail(function (data) {
                console.log(data);
            });
    });
});