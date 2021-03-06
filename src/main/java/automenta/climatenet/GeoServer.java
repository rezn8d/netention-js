/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

/**
 *
 * @author me
 */
public class GeoServer {
    
    //http://stackoverflow.com/questions/25187937/loading-geojson-layers-from-geoserver-to-leaflet-map-based-on-the-current-boundi
    //http://www.gaiaresources.com.au/json-with-geoserver-and-leaflet/
    //http://blog.georepublic.info/2012/leaflet-example-with-wfs-t/
    //  https://github.com/Georepublic/leaflet-wfs/blob/master/index.html#L137
    //https://wiki.state.ma.us/confluence/display/massgis/GeoServer+-+WFS+-+Examples
    //http://localhost:8080/WS/wfs/sdfs?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&TYPENAME=cvr01
    
    //http://docs.geoserver.org/latest/en/user/production/container.html
    //http://docs.geoserver.org/latest/en/user/production/config.html
    
    
    //http://docs.geotools.org/latest/userguide/unsupported/geojson.html
    //http://docs.codehaus.org/display/GEOTOOLS/How+to+use+a+PostGISDataStore
    
    //http://www.gdal.org/drv_libkml.html
    
    //http://geojson.org/geojson-spec.html
    //http://wiki.openstreetmap.org/wiki/Geojson_CSS
    //https://github.com/mapbox/simplestyle-spec
    
    //https://github.com/caesar0301/awesome-public-datasets
    
    /*
    embedded postgis setup:
        1. install postgres, postgis
        2. create db/
        3. in db, run: initdb
        4. edit postgresql.conf
            unix_socket_directories = './run'       # comma-separated list of directories
    
        5.  start postgis server with: postgres -D .
    
        5. create db (while server is running): createdb -p 5432 -h localhost  cv

    
        5. start postgres client: psql -p 5432 -h localhost -dcv
                command:
                    \d - prints tables
    
        add data source,
            host (localhost), port, username = 'me' (system username that created the db)

    
        6. converting KML to geoJSON
            1. convert to (flattened) KML with climatenet importer
            2. convert KML to geoJSON with ogr2ogr [this will be automated with importer]
                ogr2ogr -f "GeoJSON" output.json input.kml
    
    
        7. load geoJSON to postgis
        ogr2ogr -overwrite -f "PostGreSQL" PG:"host=localhost user=me dbname=cv" cvr01.json 

        x. load KML to postgis (doesnt work yet)
                ogr2ogr -update -append -f "PostGreSQL" PG:"host=localhost user=me dbname=cv" ccr01.kml
    
    

    
    */
}
