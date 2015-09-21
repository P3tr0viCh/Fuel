function init() {
    var yandexMap = new ymaps.Map('map', {
            center: [YandexMapJavascriptInterface.getMapCenterLatitude(),
                     YandexMapJavascriptInterface.getMapCenterLongitude()],
            zoom: 9,
            type: 'yandex#map',
            behaviors: ['multiTouch', 'drag'],
            controls: ['zoomControl']
        } , {
            suppressMapOpenBlock: true,
            suppressObsoleteBrowserNotifier: true
        }),

        searchStartPoint = new ymaps.control.SearchControl({
            options: {
                useMapBounds: true,
                noPlacemark: true,
                noPopup: true,
                placeholderContent: YandexMapJavascriptInterface.getStartSearchControlPlaceholderContent(),
                size: 'large'
            }
        }),

        calculator = new DistanceCalculator(yandexMap);

    yandexMap.controls.add(searchStartPoint);

    searchStartPoint.events.add('resultselect', function (e) {
        var results = searchStartPoint.getResultsArray(),
            selected = e.get('index'),
            point = results[selected].geometry.getCoordinates();

        calculator.setStartPoint(point);
    })
        .add('load', function (event) {
            // По полю skip определяем, что это не дозагрузка данных.
            // По getResultsCount определяем, что есть хотя бы 1 результат.
            if (!event.get('skip') && searchStartPoint.getResultsCount())
                searchStartPoint.showResult(0);
        });

    console.log('End of init');

    YandexMapJavascriptInterface.endInit();
} // init

function DistanceCalculator(map) {
    this._map = map;
    this._start = null;
    this._startBalloon;
    this._mapCenterText;
    this._mapCenterName;

    map.events.add('click', this._onClick, this);

    this.setStartPoint(map.getCenter());
}

var ptp = DistanceCalculator.prototype;

ptp._onClick = function (e) {
    console.log("_onClick");

    this.setStartPoint(e.get('coords'));
};

ptp._onStartDragEnd = function (e) {
    var coords = this._start.geometry.getCoordinates();
    this.geocode(coords);
};

ptp.setStartPoint = function (position) {
    if (this._start) this._start.geometry.setCoordinates(position);
    else {
        this._start = new ymaps.Placemark(position, { },
            { draggable: true, preset: 'islands#redDotIcon' });
        this._start.events.add('dragend', this._onStartDragEnd, this);
        this._map.geoObjects.add(this._start);
    }
    this.geocode(position);
};

ptp.geocode = function (point) {
    console.log("geocode");

    ymaps.geocode(point).then(function(geocode) {
        this._startBalloon = geocode.geoObjects.get(0) &&
            geocode.geoObjects.get(0).properties.get('balloonContentBody') || '';
        console.log(this._startBalloon);

        this._mapCenterText = geocode.geoObjects.get(0) &&
            geocode.geoObjects.get(0).properties.get('text') || ''
        console.log(this._mapCenterText);

        this._mapCenterName = geocode.geoObjects.get(0) &&
            geocode.geoObjects.get(0).properties.get('name') || ''
        console.log(this._mapCenterName);

        this.getMapCenter();
    }, this);
}

ptp.getMapCenter = function () {
    if (this._start) {
        var self = this,
            start = this._start.geometry.getCoordinates(),
            startBalloon = this._startBalloon;

            self._start.properties.set('balloonContentBody', startBalloon);

            YandexMapJavascriptInterface.updateMapCenter(
                this._mapCenterText, this._mapCenterName, start[0], start[1]);
    }
};

if (YandexMapJavascriptInterface)
    console.log('YandexMapJavascriptInterface found');
else
    console.log('YandexMapJavascriptInterface not found');

ymaps.ready(init);
