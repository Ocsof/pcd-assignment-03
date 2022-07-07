package it.unibo.pcd.distributed.view

import com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener
import it.unibo.pcd.distributed.model.{FireStation, Pluviometer, Zone, ZoneState}

import java.awt.event.{WindowAdapter, WindowEvent}
import java.awt.{Dimension, Graphics2D, RenderingHints}
import javax.swing.{BorderFactory, SwingUtilities}
import scala.swing.BorderPanel.Position.{Center, North}
import scala.swing.{Action, BorderPanel, Button, FlowPanel, Frame, Label, Panel}

trait SwingControlPanel:
  def updatePluviometer(pluviometer: Pluviometer): Unit
  def updateFireStation(fireStation: FireStation): Unit
  def freeFireStationOfZone(zoneId: Int): Unit

object SwingControlPanel:

  def apply(view: View, zones: List[Zone]): SwingControlPanel = SwingControlPanelImpl(view, zones)

  private class SwingControlPanelImpl(view: View, zones: List[Zone]) extends Frame with SwingControlPanel:
    val buttonsPanel: ButtonsPanel = ButtonsPanel(view, zones)
    val cityPanel: CityPanel = CityPanel(view.width, view.height, zones)

    title = "Control Panel"
    resizable = false
    contents = new BorderPanel{
      layout(buttonsPanel) = North
      layout(cityPanel) = Center
    }
    visible = true

    addWindowListener(new WindowAdapter() {
      override def windowClosing(ev: WindowEvent): Unit =
        System.exit(-1)

      override def windowClosed(ev: WindowEvent): Unit =
        System.exit(-1)
    })

    override def updateFireStation(fireStation: FireStation): Unit =
      SwingUtilities.invokeLater(() => {
        cityPanel.updateFireStation(fireStation)
        //todo: algoritmo per farsi restituire il button della zona della firestation, questo che ho fatto non credo sia bellino
        val freeZone: String = "Free " + fireStation.zoneId
        val button = buttonsPanel.buttons.find(_.text == freeZone).get
        if fireStation.state == ZoneState.Busy then
          button.visible = true
        repaint()
      })

    override def freeFireStationOfZone(zoneId: Int): Unit =
      SwingUtilities.invokeLater(() => {
        cityPanel.freeFireStationOfZone(zoneId)
        val freeZone: String = "Free " + zoneId.toString
        val buttonZone = buttonsPanel.buttons.find(_.text == freeZone).get
        buttonZone.visible = false
        repaint()
      })

    override def updatePluviometer(pluviometer: Pluviometer): Unit =
      SwingUtilities.invokeLater(() => {
        cityPanel.updatePluviometer(pluviometer)
        repaint()
      })

  end SwingControlPanelImpl
end SwingControlPanel

sealed class ButtonsPanel(view: View, zones: List[Zone]) extends FlowPanel:
  //mettiamo inizialmente i bottoni invisibili, e quando la firestation sarà in allarme allora
  // li renderemo visibili -> vedi metodo updateFirestation()
  var buttons: List[Button] = List.empty
  zones.foreach(zone => {
    val buttonText = "Free ".concat(zone.zoneId.toString)
    val buttonFix: Button = new Button{
      text = buttonText
      visible = false
      action = new Action(buttonText): //quando liberiamo la zona rendiamo non visibile il bottone
        override def apply(): Unit =
          view.freeZonePressed(zone.zoneId)
    }
    buttons = buttonFix :: buttons
    contents += buttonFix
  })
end ButtonsPanel

sealed class CityPanel(width: Int, height: Int, zones: List[Zone]) extends Panel:
  var fireStations: Set[FireStation] = Set()
  var pluviometers: Set[Pluviometer] = Set()

  preferredSize = Dimension(width, height)

  override def paint(g: Graphics2D): Unit =
    val g2: Graphics2D = g
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2.drawRect(0, 0, width - 1, height - 1)
    g2.setColor(java.awt.Color.BLUE)
    zones.foreach(zone => {
      val stateOption = fireStations.find(_.zoneId == zone.zoneId).map(_.state)
      val state = stateOption.getOrElse(ZoneState.Free)
      state match
        case ZoneState.Busy => g2.setColor(java.awt.Color.RED)
        case _ => g2.setColor(java.awt.Color.GREEN)
      g2.fillRect(zone.boundary.x0, zone.boundary.y0, zone.boundary.width, zone.boundary.height)
      g2.setColor(java.awt.Color.BLACK)
      g2.drawString(s"ZONE ${zone.zoneId}: ${state.toString}", zone.boundary.x0 + 5, zone.boundary.y0 + 15)
      g2.drawRect(zone.boundary.x0, zone.boundary.y0, zone.boundary.width, zone.boundary.height)
    })
    g2.setColor(java.awt.Color.BLACK)
    pluviometers.foreach(pluviometer => {
      g2.fillOval(pluviometer.position.x, pluviometer.position.y, 10, 10)
    })
    fireStations.foreach(fireStation => {
      g2.fillRect(fireStation.position.x, fireStation.position.y, 10, 10)
      g2.drawString(fireStation.state.toString, fireStation.position.x, fireStation.position.y + 20)
    })
  end paint

  def updatePluviometer(pluviometer: Pluviometer): Unit =
    if !pluviometers.contains(pluviometer) then
      this.pluviometers = this.pluviometers + pluviometer
  def updateFireStation(fireStation: FireStation): Unit = //togliamo sempre quello vecchio se avrà cambiato di stato e aggiungiamo il nuovo
    if fireStations.exists(_.position == fireStation.position) then
      this.fireStations = this.fireStations.filter(_.position != fireStation.position)
    this.fireStations = this.fireStations + fireStation
  def freeFireStationOfZone(zoneId: Int): Unit =
    val option = this.fireStations.find(_.zoneId == zoneId)
    option match
      case Some(fireStation) =>
        this.fireStations = this.fireStations - fireStation
        this.fireStations = this.fireStations + FireStation(fireStation, ZoneState.Free)
      case _ =>
end CityPanel
