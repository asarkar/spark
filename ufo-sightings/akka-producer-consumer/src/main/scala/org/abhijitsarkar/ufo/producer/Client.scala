package org.abhijitsarkar.ufo.producer

import java.time.YearMonth
import java.time.format.DateTimeFormatter

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.HttpResponse
import akka.stream.SourceShape
import akka.stream.scaladsl.{GraphDSL, Source}

/**
  * @author Abhijit Sarkar
  */
trait Client {
  def sightings(yearMonth: YearMonth): Source[(HttpResponse, YearMonth), NotUsed]
}

trait HttpClient extends Client {
  implicit def system: ActorSystem

  private val yearMonthFormatter = DateTimeFormatter.ofPattern("yyyyMM")

  override def sightings(yearMonth: YearMonth): Source[(HttpResponse, YearMonth), NotUsed] = {
    Source.fromGraph(GraphDSL.create() { implicit b: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // prepare graph elements
      val uri = s"/webreports/ndxe${yearMonth.format(yearMonthFormatter)}.html"
      val src = Source.single(RequestBuilding.Get(uri))
      lazy val conn = Http().outgoingConnection("www.nuforc.org")
        .map((_, yearMonth))

      val flow = b.add(conn)

      // connect the graph
      src ~> flow

      // expose port
      SourceShape(flow.out)
    })
  }
}
