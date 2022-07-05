package it.unibo.pcd.distributed.view

import com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener
import it.unibo.pcd.distributed.model.{FireStation, Pluviometer, Zone}

import java.awt.event.{WindowAdapter, WindowEvent}
import java.awt.{Dimension, Graphics2D, RenderingHints}
import javax.swing.{BorderFactory, SwingUtilities}
import scala.swing.BorderPanel.Position.{Center, North}
import scala.swing.{Action, BorderPanel, Button, FlowPanel, Frame, Label, Panel}

trait SwingControlPanel:
  def updatePluviometer(pluviometer: Pluviometer): Unit
  def updateFireStation(fireStation: FireStation): Unit

object SwingControlPanel:

  def apply(view: View, zones: List[Zone]): SwingControlPanel = SwingControlPanelImpl(view, zones)

  private class SwingControlPanelImpl(view: View, zones: List[Zone]) extends Frame with SwingControlPanel:
    val buttonsPanel: ButtonsPanel = ButtonsPanel(view)
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
        repaint()
      })

    override def updatePluviometer(pluviometer: Pluviometer): Unit =
      SwingUtilities.invokeLater(() => {
        cityPanel.updatePluviometer(pluviometer)
        repaint()
      })

  end SwingControlPanelImpl
end SwingControlPanel

sealed class ButtonsPanel(view: View) extends FlowPanel:
  val buttonFix: Button = new Button{
    text = "Fix Zone"
    visible = false
    action = new Action("Fix Zone"):
      override def apply(): Unit =
        visible = false
        view.fixZonePressed()
  }
  val zoneIdLabel: Label = Label(s"Zone ${view.zoneId}")
  contents += zoneIdLabel
  contents += buttonFix

end ButtonsPanel

sealed class CityPanel(width: Int, height: Int, zones: List[Zone]) extends Panel:
  var fireStations: Set[FireStation] = Set()
  val pluviometers: Set[Pluviometer] = Set()

  preferredSize = Dimension(width, height)

  override def paint(g: Graphics2D): Unit =
    val g2: Graphics2D = g
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2.drawRect(0, 0, width - 1, height - 1)
    g2.setColor(java.awt.Color.BLUE)
    zones.foreach(zone => {
      fireStations.filter(_.zoneId == zone.id).map(_.state) match
        case ZoneState.Free => g2.setColor(java.awt.Color.GREEN)
        case ZoneState.Busy => g2.setColor(java.awt.Color.RED)
      g2.fillRect(zone.bounds.x0, zone.bounds.y0, zone.bounds.width, zone.bounds.height)
      g2.setColor(java.awt.Color.BLACK)
      g2.drawString(s"ZONE ${zone.id}: ${zone.state.toString}", zone.bounds.x0 + 5, zone.bounds.y0 + 15)
      g2.drawRect(zone.bounds.x0, zone.bounds.y0, zone.bounds.width, zone.bounds.height)
    })
    g2.setColor(java.awt.Color.BLACK)
    pluviometers.foreach(pluviometer => g2.fillOval(pluviometer.position.x, pluviometer.position.y, 10, 10))
    fireStations.foreach(fireStation => {
      g2.fillRect(fireStation.position.x, fireStation.position.y, 10, 10)
      g2.drawString(fireStation.state.toString, fireStation.position.x, fireStation.position.y + 20)
    })
  end paint

  def updatePluviometer(pluviometer: Pluviometer): Unit =
    if(!pluviometers.contains(pluviometer))
      this.pluviometers = this.pluviometers + pluviometer
  def updateFireStation(fireStation: FireStation): Unit =
    if(!fireStations.contains(fireStation))
    this.fireStations = this.fireStations + fireStation

end CityPanel
