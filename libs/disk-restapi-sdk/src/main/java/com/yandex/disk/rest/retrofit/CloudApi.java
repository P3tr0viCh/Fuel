/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest.retrofit;

import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.json.ApiVersion;
import com.yandex.disk.rest.json.DiskInfo;
import com.yandex.disk.rest.json.Link;
import com.yandex.disk.rest.json.Operation;
import com.yandex.disk.rest.json.Resource;
import com.yandex.disk.rest.json.ResourceList;

import java.io.IOException;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CloudApi {

    @GET("/")
    Call<ApiVersion> getApiVersion()
            throws IOException, ServerIOException;

    @GET("/v1/disk/operations/{operation_id}")
    Call<Operation> getOperation(@Path("operation_id") String operationId)
            throws IOException, ServerIOException;

    @GET("/v1/disk")
    Call<DiskInfo> getDiskInfo(@Query("fields") String fields)
            throws IOException, ServerIOException;

    @GET("/v1/disk/resources")
    Call<Resource> getResources(@Query("path") String path, @Query("fields") String fields,
                                @Query("limit") Integer limit, @Query("offset") Integer offset,
                                @Query("sort") String sort, @Query("preview_size") String previewSize,
                                @Query("preview_crop") Boolean previewCrop)
            throws IOException, ServerIOException;

    @GET("/v1/disk/resources/files")
    Call<ResourceList> getFlatResourceList(@Query("limit") Integer limit, @Query("media_type") String mediaType,
                                           @Query("offset") Integer offset, @Query("fields") String fields,
                                           @Query("preview_size") String previewSize,
                                           @Query("preview_crop") Boolean previewCrop)
            throws IOException, ServerIOException;

    @GET("/v1/disk/resources/last-uploaded")
    Call<ResourceList> getLastUploadedResources(@Query("limit") Integer limit, @Query("media_type") String mediaType,
                                                @Query("offset") Integer offset, @Query("fields") String fields,
                                                @Query("preview_size") String previewSize,
                                                @Query("preview_crop") Boolean previewCrop)
            throws IOException, ServerIOException;

    @PATCH("/v1/disk/resources/")
    Call<Resource> patchResource(@Query("path") String path, @Query("fields") String fields,
                                 @Body RequestBody body)
            throws IOException, ServerIOException;

    @GET("/v1/disk/resources/download")
    Call<Link> getDownloadLink(@Query("path") String path)
            throws IOException, ServerIOException;

    @POST("/v1/disk/resources/upload")
    Call<Link> saveFromUrl(@Query("url") String url, @Query("path") String path)
            throws IOException, ServerIOException;

    @GET("/v1/disk/resources/upload")
    Call<Link> getUploadLink(@Query("path") String path, @Query("overwrite") Boolean overwrite)
            throws IOException, ServerIOException;

    @POST("/v1/disk/resources/copy")
    Call<Link> copy(@Query("from") String from, @Query("path") String path,
                    @Query("overwrite") Boolean overwrite)
            throws IOException, ServerIOException;

    @POST("/v1/disk/resources/move")
    Call<Link> move(@Query("from") String from, @Query("path") String path,
                    @Query("overwrite") Boolean overwrite)
            throws IOException, ServerIOException;

    @PUT("/v1/disk/resources")
    Call<Link> makeFolder(@Query("path") String path)
            throws IOException, ServerIOException;

    @PUT("/v1/disk/resources/publish")
    Call<Link> publish(@Query("path") String path)
            throws IOException, ServerIOException;

    @PUT("/v1/disk/resources/unpublish")
    Call<Link> unpublish(@Query("path") String path)
            throws IOException, ServerIOException;

    @GET("/v1/disk/public/resources")
    Call<Resource> listPublicResources(@Query("public_key") String publicKey, @Query("path") String path,
                                       @Query("fields") String fields, @Query("limit") Integer limit,
                                       @Query("offset") Integer offset, @Query("sort") String sort,
                                       @Query("preview_size") String previewSize,
                                       @Query("preview_crop") Boolean previewCrop)
            throws IOException, ServerIOException;

    @GET("/v1/disk/public/resources/download")
    Call<Link> getPublicResourceDownloadLink(@Query("public_key") String publicKey,
                                             @Query("path") String path)
            throws IOException, ServerIOException;

    @POST("/v1/disk/public/resources/save-to-disk/")
    Call<Link> savePublicResource(@Query("public_key") String publicKey, @Query("path") String path,
                                  @Query("name") String name)
            throws IOException, ServerIOException;

    @GET("/v1/disk/trash/resources")
    Call<Resource> getTrashResources(@Query("path") String path, @Query("fields") String fields,
                                     @Query("limit") Integer limit, @Query("offset") Integer offset,
                                     @Query("sort") String sort, @Query("preview_size") String previewSize,
                                     @Query("preview_crop") Boolean previewCrop)
            throws IOException, ServerIOException;
}
