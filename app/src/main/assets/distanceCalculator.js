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

        calculator = new DistanceCalculator(yandexMap);

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

function DistanceCalculator(map) {
    this.map = map;
    this.start = null;
    this.geoObject = null;
    this.route = null;
    this.startBalloon;
    this.finishBalloon;

    map.events.add('click', this.onClick, this);

    this.setStartPoint(map.getCenter());
}

var ptp = DistanceCalculator.prototype;

ptp.onClick = function (e) {
    console.log("onClick");

    if (this.start) this.setFinishPoint(e.get('coords'));
    else            this.setStartPoint(e.get('coords'));
}

ptp.onStartDragEnd = function (e) {
    this.geocode("start", this.start.geometry.getCoordinates());
}

ptp.onFinishDragEnd = function (e) {
    this.geocode("finish", this.finish.geometry.getCoordinates());
}

ptp.setStartPoint = function (position) {
    if (this.start) this.start.geometry.setCoordinates(position);
    else {
        this.start = new ymaps.Placemark(position, { },
            { draggable: true, preset: 'islands#redDotIcon' });
        this.start.events.add('dragend', this.onStartDragEnd, this);
        this.map.geoObjects.add(this.start);
    }
    this.geocode("start", position);
}

ptp.setFinishPoint = function (position) {
    if (this.finish) this.finish.geometry.setCoordinates(position);
    else {
        this.finish = new ymaps.Placemark(position, { },
            { draggable: true, preset: 'islands#darkGreenDotIcon' });
        this.finish.events.add('dragend', this.onFinishDragEnd, this);
        this.map.geoObjects.add(this.finish);
    }
    if (this.start) this.geocode("finish", position);
}

ptp.geocode = function (str, point) {
    ymaps.geocode(point).then(function(geocode) {
        geoObject = geocode.geoObjects.get(0);

        if (str == "start") {
            startBalloon = geoObject && geoObject.properties.get('balloonContent') ||
                YandexMapJavascriptInterface.getEmptyBalloonContent();
            this.start.properties.set('balloonContentBody', startBalloon);
            console.log(str + ": " + startBalloon);
        } else {
            finishBalloon = geoObject && geoObject.properties.get('balloonContent') ||
                YandexMapJavascriptInterface.getEmptyBalloonContent();
            this.finish.properties.set('balloonContentBody', finishBalloon);
            console.log(str + ": " + finishBalloon);
        }
        this.getDirection();
    }, this);
}

ptp.getDirection = function () {
    if (this.route) this.map.geoObjects.remove(this.route);

    if (this.start && this.finish) {
        var self = this,
            startCoordinates = this.start.geometry.getCoordinates(),
            finishCoordinates = this.finish.geometry.getCoordinates();

        ymaps.route([startCoordinates, finishCoordinates])
            .then(function (router) {
                var distance = Math.round(router.getLength() / 1000);

                console.log("distance: " + distance);

                self.route = router.getPaths();

                self.route.options.set({ strokeWidth: 5, strokeColor: '0000ffff', opacity: 0.5 });
                self.map.geoObjects.add(self.route);

                YandexMapJavascriptInterface.updateDistance(distance);
            }, function (err) {
                 console.log("error: " + err.message);
                 YandexMapJavascriptInterface.errorConstructRoute();
            });

        self.map.setBounds(self.map.geoObjects.getBounds())
    }
}

if (YandexMapJavascriptInterface)
    console.log('YandexMapJavascriptInterface found');
else
    console.log('YandexMapJavascriptInterface not found');

ymaps.ready(init);
