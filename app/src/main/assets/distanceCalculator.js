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

        searchFinishPoint = new ymaps.control.SearchControl({
            options: {
                useMapBounds: true,
                noCentering: true,
                noPopup: true,
                noPlacemark: true,
                placeholderContent: YandexMapJavascriptInterface.getFinishSearchControlPlaceholderContent(),
                size: 'large',
                float: 'none',
                position: { left: 10, top: 44 }
            }
        }),

        calculator = new DistanceCalculator(yandexMap, yandexMap.getCenter());

    yandexMap.controls.add(searchStartPoint);
    yandexMap.controls.add(searchFinishPoint);

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

    searchFinishPoint.events.add('resultselect', function (e) {
        var results = searchFinishPoint.getResultsArray(),
            selected = e.get('index'),
            point = results[selected].geometry.getCoordinates();

        calculator.setFinishPoint(point);
    })
        .add('load', function (event) {
            // По полю skip определяем, что это не дозагрузка данных.
            // По getResultsCount определяем, что есть хотя бы 1 результат.
            if (!event.get('skip') && searchFinishPoint.getResultsCount())
                searchFinishPoint.showResult(0);
        });

    console.log('End of init');

    YandexMapJavascriptInterface.endInit();
} // init

function DistanceCalculator(map, finish) {
    this._map = map;
    this._start = null;
    this._route = null;
    this._startBalloon;
    this._finishBalloon;

    map.events.add('click', this._onClick, this);
}

var ptp = DistanceCalculator.prototype;

ptp._onClick = function (e) {
    console.log("_onClick");

    if (this._start) this.setFinishPoint(e.get('coords'));
    else             this.setStartPoint(e.get('coords'));
};

ptp._onStartDragEnd = function (e) {
    var coords = this._start.geometry.getCoordinates();
    this.geocode("start", coords);
}

ptp._onFinishDragEnd = function (e) {
    var coords = this._finish.geometry.getCoordinates();
    this.geocode("finish", coords);
}

ptp.getDirection = function () {
    if (this._route) this._map.geoObjects.remove(this._route);

    if (this._start && this._finish) {
        var self = this,
            start = this._start.geometry.getCoordinates(),
            finish = this._finish.geometry.getCoordinates(),
            startBalloon = this._startBalloon,
            finishBalloon = this._finishBalloon;

        ymaps.route([start, finish])
            .then(function (router) {
                var distance = Math.round(router.getLength() / 1000);

                console.log("route: " + distance);

                self._route = router.getPaths();

                self._route.options.set({ strokeWidth: 5, strokeColor: '0000ffff', opacity: 0.5 });
                self._map.geoObjects.add(self._route);

                self._start.properties.set('balloonContentBody', startBalloon);
                self._finish.properties.set('balloonContentBody', finishBalloon);

                YandexMapJavascriptInterface.updateDistance(distance);
            });

        self._map.setBounds(self._map.geoObjects.getBounds())
    }
};

ptp.setStartPoint = function (position) {
    if (this._start) this._start.geometry.setCoordinates(position);
    else {
        this._start = new ymaps.Placemark(position, { },
            { draggable: true, preset: 'islands#redDotIcon' });
        this._start.events.add('dragend', this._onStartDragEnd, this);
        this._map.geoObjects.add(this._start);
    }
    this.geocode("start", position);
};

ptp.setFinishPoint = function (position) {
    if (this._finish) this._finish.geometry.setCoordinates(position);
    else {
        this._finish = new ymaps.Placemark(position, { },
            { draggable: true, preset: 'islands#darkGreenDotIcon' });
        this._finish.events.add('dragend', this._onFinishDragEnd, this);
        this._map.geoObjects.add(this._finish);
    }
    if (this._start) this.geocode("finish", position);
};

ptp.geocode = function (str, point) {
    ymaps.geocode(point).then(function(geocode) {
        if (str == "start") {
            this._startBalloon = geocode.geoObjects.get(0) &&
                geocode.geoObjects.get(0).properties.get('balloonContentBody') || '';
            console.log(str + " " + this._startBalloon);
        } else {
            this._finishBalloon = geocode.geoObjects.get(0) &&
                geocode.geoObjects.get(0).properties.get('balloonContentBody') || '';
            console.log(str + " " + this._finishBalloon);
        }
        this.getDirection();
    }, this);
}

if (YandexMapJavascriptInterface)
    console.log('YandexMapJavascriptInterface found');
else
    console.log('YandexMapJavascriptInterface not found');

ymaps.ready(init);
