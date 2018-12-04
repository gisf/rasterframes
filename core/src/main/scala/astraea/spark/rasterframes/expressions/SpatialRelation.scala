/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright 2017 Astraea, Inc.
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
 */

package astraea.spark.rasterframes.expressions

import astraea.spark.rasterframes.expressions.SpatialRelation.RelationPredicate
import com.vividsolutions.jts.geom._
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.catalyst.expressions.codegen.CodegenFallback
import org.apache.spark.sql.catalyst.expressions.{ScalaUDF, _}
import org.apache.spark.sql.types._
import org.locationtech.geomesa.spark.jts.udf.SpatialRelationFunctions._



/**
 * Determine if two spatial constructs intersect each other.
 *
 * @since 12/28/17
 */
abstract class SpatialRelation extends BinaryExpression
  with CodegenFallback with GeomDeserializerSupport  {
  lazy val jtsPointEncoder = ExpressionEncoder[Point]()

  override def toString: String = s"$nodeName($left, $right)"

  override def dataType: DataType = BooleanType

  override def nullable: Boolean = left.nullable || right.nullable

  override protected def nullSafeEval(leftEval: Any, rightEval: Any): java.lang.Boolean = {
    val leftGeom = extractGeometry(left, leftEval)
    val rightGeom = extractGeometry(right, rightEval)
    relation(leftGeom, rightGeom)
  }

  val relation: RelationPredicate
}

object SpatialRelation {
  type RelationPredicate = (Geometry, Geometry) ⇒ java.lang.Boolean

  case class Intersects(left: Expression, right: Expression) extends SpatialRelation {
    override def nodeName = "intersects"
    val relation = ST_Intersects
  }
  case class Contains(left: Expression, right: Expression) extends SpatialRelation {
    override def nodeName = "contains"
    val relation = ST_Contains
  }
  case class Covers(left: Expression, right: Expression) extends SpatialRelation {
    override def nodeName = "covers"
    val relation = ST_Covers
  }
  case class Crosses(left: Expression, right: Expression) extends SpatialRelation {
    override def nodeName = "crosses"
    val relation = ST_Crosses
  }
  case class Disjoint(left: Expression, right: Expression) extends SpatialRelation {
    override def nodeName = "disjoint"
    val relation = ST_Disjoint
  }
  case class Overlaps(left: Expression, right: Expression) extends SpatialRelation {
    override def nodeName = "overlaps"
    val relation = ST_Overlaps
  }
  case class Touches(left: Expression, right: Expression) extends SpatialRelation {
    override def nodeName = "touches"
    val relation = ST_Touches
  }
  case class Within(left: Expression, right: Expression) extends SpatialRelation {
    override def nodeName = "within"
    val relation = ST_Within
  }

  private val predicateMap = Map(
    ST_Intersects -> Intersects,
    ST_Contains -> Contains,
    ST_Covers -> Covers,
    ST_Crosses -> Crosses,
    ST_Disjoint -> Disjoint,
    ST_Overlaps -> Overlaps,
    ST_Touches -> Touches,
    ST_Within -> Within
  )

  def fromUDF(udf: ScalaUDF) = {
    udf.function match {
      case rp: RelationPredicate @unchecked ⇒
        predicateMap.get(rp).map(_.apply(udf.children.head, udf.children.last))
      case _ ⇒ None
    }
  }
}