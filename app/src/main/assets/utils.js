const MAP_TYPE_DISTANCE = "distance";
const MAP_TYPE_CENTER = "center";
const MAP_TYPE_UNDEFINED = "undefined";

function getMapType() {
    var query = window.location.search;
    var index = query.indexOf("?");

    if (index > -1)
        return query.substring(index + 1, query.length);
    else
        return MAP_TYPE_UNDEFINED;
}

var mapType = getMapType();

console.log("mapType == " + mapType);