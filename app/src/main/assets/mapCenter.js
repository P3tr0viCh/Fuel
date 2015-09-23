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
    this.map = map;
    this.start = null;
    this.geoObject = null;
    this.startBalloon = null;

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

    map.events.add('click', this.onClick, this);

    this.setStartPoint(map.getCenter());
}

var ptp = DistanceCalculator.prototype;

ptp.onClick = function (e) {
    console.log("onClick");
    this.setStartPoint(e.get('coords'));
}

ptp.onStartDragEnd = function (e) {
    this.geocode(this.start.geometry.getCoordinates());
}

ptp.setStartPoint = function (position) {
    if (this.start) this.start.geometry.setCoordinates(position);
    else {
        this.start = new ymaps.Placemark(position, { },
            { draggable: true, preset: 'islands#redDotIcon' });
        this.start.events.add('dragend', this.onStartDragEnd, this);
        this.map.geoObjects.add(this.start);
    }
    this.geocode(position);
}

ptp.geocode = function (point) {
    ymaps.geocode(point).then(function(geocode) {
        geoObject = geocode.geoObjects.get(0);

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
    }, this);
}

ptp.getAllKeys = function (object) {
    for (var key in object)
        if (object[key] == '[object Object]')
            this.getAllKeys(object[key])
        else {
            console.log('getAll: key == ' + key + ', value == ' + object[key]);
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

        YandexMapJavascriptInterface.updateMapCenter(
            mapCenterText, mapCenterTitle, mapCenterSubtitle,
            startCoordinates[0], startCoordinates[1]);
    }
}

if (YandexMapJavascriptInterface)
    console.log('YandexMapJavascriptInterface found');
else
    console.log('YandexMapJavascriptInterface not found');

ymaps.ready(init);
