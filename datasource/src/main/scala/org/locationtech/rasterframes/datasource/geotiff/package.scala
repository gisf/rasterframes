/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright 2018 Astraea, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     [http://www.apache.org/licenses/LICENSE-2.0]
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.locationtech.rasterframes.datasource
import java.net.URI

import org.apache.spark.sql.{DataFrameReader, DataFrameWriter}
import org.locationtech.rasterframes._
import _root_.geotrellis.proj4.CRS
import shapeless.tag.@@
import shapeless.tag

package object geotiff {
  /** Tagged type construction for enabling type-safe extension methods for loading
   * a RasterFrameLayer from a single GeoTiff. */
  type GeoTiffRasterFrameReader = DataFrameReader @@ GeoTiffRasterFrameReaderTag
  trait GeoTiffRasterFrameReaderTag

  /** Tagged type construction for enabling type-safe extension methods for writing
    * a RasterFrame to a geotiff. */
  type GeoTiffRasterFrameWriter[T] = DataFrameWriter[T] @@ GeoTiffRasterFrameWriterTag
  trait GeoTiffRasterFrameWriterTag

  /** Adds `geotiff` format specifier to `DataFrameReader`. */
  implicit class DataFrameReaderHasGeoTiffFormat(val reader: DataFrameReader) {
    @deprecated("Use `raster` instead.", "7/1/2019")
    def geotiff: GeoTiffRasterFrameReader =
      tag[GeoTiffRasterFrameReaderTag][DataFrameReader](
        reader.format(GeoTiffDataSource.SHORT_NAME)
      )
  }

  implicit class DataFrameWriterHasGeoTiffFormat[T](val writer: DataFrameWriter[T]) {
    def geotiff: GeoTiffRasterFrameWriter[T] =
      tag[GeoTiffRasterFrameWriterTag][DataFrameWriter[T]](
        writer.format(GeoTiffDataSource.SHORT_NAME)
      )

    def withDimensions(cols: Int, rows: Int): GeoTiffRasterFrameWriter[T] =
      tag[GeoTiffRasterFrameWriterTag][DataFrameWriter[T]](
        writer
          .option(GeoTiffDataSource.IMAGE_WIDTH_PARAM, cols)
          .option(GeoTiffDataSource.IMAGE_HEIGHT_PARAM, rows)
      )

    def withCompression: GeoTiffRasterFrameWriter[T] =
      tag[GeoTiffRasterFrameWriterTag][DataFrameWriter[T]](
        writer
          .option(GeoTiffDataSource.COMPRESS_PARAM, true)
      )
    def withCRS(crs: CRS): GeoTiffRasterFrameWriter[T] =
      tag[GeoTiffRasterFrameWriterTag][DataFrameWriter[T]](
        writer
          .option(GeoTiffDataSource.CRS_PARAM, crs.toProj4String)
      )
  }

  /** Adds `loadLayer` to appropriately tagged `DataFrameReader` */
  implicit class GeoTiffReaderWithRF(val reader: GeoTiffRasterFrameReader) {
    @deprecated("Use `raster` instead.", "7/1/2019")
    def loadLayer(path: URI): RasterFrameLayer = reader.load(path.toASCIIString).asLayer
  }
}
