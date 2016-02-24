var calculator;

function init() {
    try {
        var yandexMap = new ymaps.Map('map', {
                center: [YandexMapJavascriptInterface.getMapCenterLatitude(),
                         YandexMapJavascriptInterface.getMapCenterLongitude()],
                zoom: 9,
                type: 'yandex#map',
                behaviors: ['multiTouch', 'drag'],
                controls: []
            }, {
                suppressMapOpenBlock: true,
                suppressObsoleteBrowserNotifier: true
        });

        var zoomControl = new ymaps.control.ZoomControl({
                options: {
                    size: 'large',
                    float: 'none',
                    position: {
                         left: YandexMapJavascriptInterface.getZoomControlLeft(),
                         top:  YandexMapJavascriptInterface.getZoomControlTop()
                    }
                }
        });

        yandexMap.controls.add(zoomControl);

        var searchStartPoint = new ymaps.control.SearchControl({
                options: {
                    useMapBounds: true,
                    noPlacemark: true,
                    noPopup: true,
                    suppressYandexSearch: true,
                    placeholderContent: YandexMapJavascriptInterface.getStartSearchControlPlaceholderContent(),
                    size: 'large',
                    float: 'none',
                    position: {
                        left: YandexMapJavascriptInterface.getStartSearchControlLeft(),
                        top:  YandexMapJavascriptInterface.getStartSearchControlTop()
                    }
                }
        });

        searchStartPoint.events.add('resultselect', function (e) {
            var results = searchStartPoint.getResultsArray(),
                selected = e.get('index'),
                point = results[selected].geometry.getCoordinates();

            calculator.setStartPoint(point);
        })
            .add('load', function (event) {
                if (!event.get('skip')) {
                    if (searchStartPoint.getResultsCount())
                        searchStartPoint.showResult(0);
                    else
                        YandexMapJavascriptInterface.onErrorSearchPoint();
                }
            });

        yandexMap.controls.add(searchStartPoint);

        if (mapType == MAP_TYPE_DISTANCE) {
            var searchFinishPoint = new ymaps.control.SearchControl({
                    options: {
                        useMapBounds: true,
                        noCentering: true,
                        noPopup: true,
                        noPlacemark: true,
                        suppressYandexSearch: true,
                        placeholderContent: YandexMapJavascriptInterface.getFinishSearchControlPlaceholderContent(),
                        size: 'large',
                        float: 'none',
                        position: {
                            left: YandexMapJavascriptInterface.getFinishSearchControlLeft(),
                            top:  YandexMapJavascriptInterface.getFinishSearchControlTop()
                        }
                    }
            });

            searchFinishPoint.events.add('resultselect', function (e) {
                var results = searchFinishPoint.getResultsArray(),
                    selected = e.get('index'),
                    point = results[selected].geometry.getCoordinates();

                calculator.setFinishPoint(point);
            })
                .add('load', function (event) {
                    if (!event.get('skip')) {
                        if (searchFinishPoint.getResultsCount())
                            searchFinishPoint.showResult(0);
                        else
                            YandexMapJavascriptInterface.onErrorSearchPoint();
                    }
                });

            yandexMap.controls.add(searchFinishPoint);
        }

        calculator = new DistanceCalculator(yandexMap);
    } finally {
        console.log('End of init');

        YandexMapJavascriptInterface.onEndLoading(false);
    }
} // init

function DistanceCalculator(map) {
    this.map = map;
    this.start = null;
    this.geoObject = null;
    this.route = null;
    this.startBalloon = null;
    this.finishBalloon = null;

    if (mapType == MAP_TYPE_CENTER) {
        this.mapCenterText;
        this.mapCenterTitle;
        this.mapCenterSubtitle;

        this.AdministrativeAreaName;
        this.SubAdministrativeAreaName;
        this.LocalityName;
        this.DependentLocalityName;
        this.ThoroughfareName;
        this.PremiseName;
        this.PremiseNumber;
    }

    map.events.add('click', this.onClick, this);

    this.setStartPoint(map.getCenter());
}

var ptp = DistanceCalculator.prototype;

ptp.onClick = function (e) {
    console.log("onClick");

    switch (mapType) {
        case MAP_TYPE_CENTER:
            this.setStartPoint(e.get('coords'));
            break;
        case MAP_TYPE_DISTANCE:
            if (this.start) this.setFinishPoint(e.get('coords'));
            else            this.setStartPoint(e.get('coords'));
            break;
    }
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

        switch (mapType) {
        case MAP_TYPE_CENTER:
            mapCenterText = geoObject && geoObject.properties.get('text') || '';

            if (mapCenterText != '') {
                startBalloon = geoObject && geoObject.properties.get('balloonContent') || '';

                console.log('balloonContent: ' + startBalloon);

                console.log('text: ' + mapCenterText);

                AdministrativeAreaName = null;
                SubAdministrativeAreaName = null;
                LocalityName = null;
                DependentLocalityName = null;
                ThoroughfareName = null;
                PremiseName = null;
                PremiseNumber = null;

                mapCenterTitle = null;
                mapCenterSubtitle = null;

                this.getAllKeys(geoObject.properties.get('metaDataProperty.GeocoderMetaData.AddressDetails'));

                if (LocalityName != null) {
                    mapCenterTitle = LocalityName;
                    if (ThoroughfareName != null) {
                        mapCenterSubtitle = ThoroughfareName;
                    } else if (PremiseName != null) {
                        mapCenterSubtitle = PremiseName;
                    } else if (DependentLocalityName != null) {
                        mapCenterSubtitle = DependentLocalityName;
                    }
                    if (mapCenterSubtitle != null) {
                        if (PremiseNumber != null)
                            mapCenterSubtitle = mapCenterSubtitle + ', ' + PremiseNumber;
                    } else {
                        if (AdministrativeAreaName != null) {
                            mapCenterTitle = AdministrativeAreaName;
                            mapCenterSubtitle = LocalityName;
                        }
                    }
                } else {
                    if (PremiseName != null) {
                        mapCenterSubtitle = PremiseName;
                    } else if (ThoroughfareName != null) {
                        mapCenterSubtitle = ThoroughfareName;
                    }
                    if (mapCenterSubtitle != null) {
                        if (SubAdministrativeAreaName != null)
                            mapCenterTitle = SubAdministrativeAreaName;
                        else if (AdministrativeAreaName != null)
                            mapCenterTitle = AdministrativeAreaName;
                    } else {
                        if (AdministrativeAreaName != null)
                            mapCenterTitle = AdministrativeAreaName;
                        if (SubAdministrativeAreaName != null)
                            mapCenterSubtitle = SubAdministrativeAreaName;
                    }
                }
                if (mapCenterTitle == null) {
                    if (mapCenterSubtitle == null) {
                        mapCenterTitle = '';
                        mapCenterSubtitle = '';
                    } else {
                        mapCenterTitle = mapCenterSubtitle;
                        mapCenterSubtitle = '';
                    }
                } else if (mapCenterSubtitle == null) mapCenterSubtitle = '';
            } else {
                console.log('text: null');
                startBalloon = YandexMapJavascriptInterface.getEmptyBalloonContent();
                mapCenterTitle = '';
                mapCenterSubtitle = '';
            }

            this.getMapCenter();

            break;
        case MAP_TYPE_DISTANCE:
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

            break;
        }
    }, this);
}

ptp.getAllKeys = function (object) {
    for (var key in object)
        if (object[key] == '[object Object]')
            this.getAllKeys(object[key])
        else {
            // console.log('getAll: key == ' + key + ', value == ' + object[key]);
            switch (key) {
                case 'AdministrativeAreaName': AdministrativeAreaName = object[key]; break;
                case 'SubAdministrativeAreaName': SubAdministrativeAreaName = object[key]; break;
                case 'LocalityName': LocalityName = object[key]; break;
                case 'DependentLocalityName': DependentLocalityName = object[key]; break;
                case 'ThoroughfareName': ThoroughfareName = object[key]; break;
                case 'PremiseName': PremiseName = object[key]; break;
                case 'PremiseNumber': PremiseNumber = object[key]; break;
            }
        }
}

ptp.getMapCenter = function () {
    if (this.start) {
        var startCoordinates = this.start.geometry.getCoordinates();

        this.start.properties.set('balloonContentBody', startBalloon);

        YandexMapJavascriptInterface.onMapCenterChange(
            mapCenterText, mapCenterTitle, mapCenterSubtitle,
            startCoordinates[0], startCoordinates[1]);
    }
}

ptp.getDirection = function () {
    if (this.route) this.map.geoObjects.remove(this.route);

    if (this.start && this.finish) {
        var self = this,
            startCoordinates = this.start.geometry.getCoordinates(),
            finishCoordinates = this.finish.geometry.getCoordinates();

        ymaps.route([startCoordinates, finishCoordinates], { mapStateAutoApply: true })
            .then(function (router) {
                var distance = Math.round(router.getLength() / 1000);

                console.log("distance == " + distance);

                self.route = router.getPaths();

                self.route.options.set({ strokeWidth: 5, strokeColor: "0000FFFF", opacity: 0.5 });

                self.map.geoObjects.add(self.route);

                YandexMapJavascriptInterface.onDistanceChange(distance);
            }, function (err) {
                 console.log("getDirection error: " + err.toString());
                 YandexMapJavascriptInterface.onErrorConstructRoute();
            });

        self.map.setBounds(self.map.geoObjects.getBounds())
    }
}

function performGeolocation() {
    ymaps.geolocation.get().then(function (res) {
        var position = res.geoObjects.get(0).geometry.getCoordinates();

        calculator.map.panTo(position);

        calculator.setStartPoint(position);
    }, function (err) {
        console.log("performGeolocation error: " + err.toString());
        YandexMapJavascriptInterface.onErrorGeolocation();
    });
}

if (YandexMapJavascriptInterface) {
    console.log("YandexMapJavascriptInterface found");

    try {
        switch (mapType) {
            case MAP_TYPE_CENTER:
            case MAP_TYPE_DISTANCE:
                break;
            default:
                throw new TypeError("Unknown map type == " + mapType);
        }

        ymaps.ready(init);
    } catch (err) {
        console.log("error == " + err.toString());
        YandexMapJavascriptInterface.onEndLoading(true);
    }
} else
    console.log("YandexMapJavascriptInterface not found");