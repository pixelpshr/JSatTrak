/*
 * OrbitModel.java
 *
 * aka ECIModelRenderable - draws models  in ECI coordinates using ECIRenderableLayer
 *=====================================================================
 * Copyright (C) 2008 Shawn E. Gano
 * 
 * This file is part of JSatTrak.
 * 
 * JSatTrak is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JSatTrak is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JSatTrak.  If not, see <http://www.gnu.org/licenses/>.
 * =====================================================================
 * 
 */

package jsattrak.utilities;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.FrameFactory;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.MultiLineTextRenderer;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.Logging;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.util.Hashtable;
import javax.media.opengl.GL;
import jsattrak.objects.AbstractSatellite;
import name.gano.astro.MathUtils;
import name.gano.worldwind.geom.Cone;
import name.gano.worldwind.geom.SphereObject;


/**
 * @author Shawn E. Gano 
 */
public class OrbitModelRenderable implements Renderable
{
    
    // hash of sat props
    Hashtable<String,AbstractSatellite> satHash;
    
    // Sphere Object
    double sphereRadius = 100000;
    SphereObject sphere = new SphereObject(new Vec4(0,0,0,0), sphereRadius, true);
    // lat long placement
    //SurfaceCircle surfCirc; 
    Cone cone;
    
    // fill transparency
    int circleViewTransparency = 122; //0-255
    
    // save globe
    Globe globe;
    
    
    /** Creates a new instance of OrbitModel
     * @param satHash
     * @param globe 
     */
    public OrbitModelRenderable(Hashtable<String,AbstractSatellite> satHash, Globe globe)
    {
        this.satHash = satHash;
        this.globe = globe;
        
        //surfCirc = new SurfaceCircle(globe,LatLon.fromRadians(0.0,0.0),1.0,32);

        
        Color transGreen = new Color(0.0f,1.0f,0.0f,circleViewTransparency/255.0f);
        cone = new Cone(globe, 45, -90, 1000000, 2000000,Angle.fromDegrees(0),Angle.fromDegrees(-90), transGreen);
        //Cone(Globe globe, double lat, double lon, double mGroundRange, double mCeiling, Angle orientation, Angle elevation, Color theColor)
    }
    
    
    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        
        javax.media.opengl.GL gl = dc.getGL();
        
        gl.glEnable(GL.GL_TEXTURE_2D);        
        gl.glPushAttrib(javax.media.opengl.GL.GL_TEXTURE_BIT | javax.media.opengl.GL.GL_ENABLE_BIT | javax.media.opengl.GL.GL_CURRENT_BIT);
        gl.glMatrixMode(javax.media.opengl.GL.GL_MODELVIEW);
              
        // for each satellite
        for(AbstractSatellite sat : satHash.values() ) // search through all sat nodes
        {
            // set color
            Color satColor = sat.getSatColor();
            gl.glColor3f( satColor.getRed()/255.0f , satColor.getGreen()/255.0f , satColor.getBlue()/255.0f ); // COLOR
            
            Double nanDbl = new Double(Double.NaN);
            
            if (sat.isShow3DOrbitTrace() && sat.isShow3DOrbitTraceECI())
            {
                // plot lag orbit
                gl.glBegin(GL.GL_LINE_STRIP); //GL_LINE_STRIP
                for (int i = 0; i < sat.getNumGroundTrackLagPts(); i++)
                {
                    // add next Mean of Date vertex
                    double[] xyz = sat.getGroundTrackXyzLagPt(i);
                    if(!nanDbl.equals(xyz[0])) // NaN check
                    {
                        gl.glVertex3f((float)-xyz[0], (float)xyz[2], (float)xyz[1]);
                    }
                }
                gl.glEnd();

                // plot lead orbit
                gl.glBegin(GL.GL_LINE_STRIP); //GL_LINE_STRIP
                for (int i = 0; i < sat.getNumGroundTrackLeadPts(); i++)
                {
                    // add next Mean of Date vertex
                    double[] xyz = sat.getGroundTrackXyzLeadPt(i);
                    if(!nanDbl.equals(xyz[0])) // NaN check
                    {
                        gl.glVertex3f((float)-xyz[0], (float)xyz[2], (float)xyz[1]);
                    }
                }
                gl.glEnd();
            } // show orbit trace
            
            // plot position 
            double[] xyz = sat.getPosMOD();
            if(xyz != null)
            {
                // 3D model is rendered Here
                if(sat.isUse3dModel())
                {
                    // custom 3D object
                    if(sat.getThreeDModel() != null) // make sure it is not null
                    {
                       //- 
                        sat.getThreeDModel().render(dc); // render model
                    }
                }
                else
                {
                    // default "sphere" for model
                    sphere.setCenter(-xyz[0], xyz[2], xyz[1]);
                    sphere.render(dc);
                }
            } // if pos is not null
            
            // draw name
            if(sat.isShow3DName())
            {
                AnnotationAttributes geoAttr = createFontAttribs(sat.getSatColor());
                GlobeAnnotation an = new GlobeAnnotation(sat.getName(), Position.fromRadians(sat.getLatitude(), sat.getLongitude(), sat.getAltitude()), geoAttr);
                // annotation - without any attribs, gives a bubble box
                // annotation doesn't strech well in GLCanvas
                //GlobeAnnotation an = new GlobeAnnotation(gs.getStationName(), Position.fromDegrees(gs.getLatitude(), gs.getLongitude(), gs.getAltitude()));
                an.render(dc);
            }
            
            // draw earth footprint
            if (sat.isShow3DFootprint())
            {
                double[] lla = sat.getLLA();
                if (lla != null)
                {
//                    surfCirc.setCenter(LatLon.fromRadians(lla[0], lla[1]));
//                    surfCirc.setRadius(calcFootPrintRadiusFromAlt(lla[2]));
//                    surfCirc.setBorderColor(satColor);
//                    surfCirc.setPaint(new Color(satColor.getRed(), satColor.getGreen(), satColor.getBlue(), circleViewTransparency)); // sets interior color
//                    surfCirc.render(dc);
                    
                    // test cone 
                    //cone.setLatLonRadians( lla[0] , lla[1], lla[2] );
                    cone.setVertexPosition(-xyz[0], xyz[2], xyz[1]);
                    double[] rh = calcConeRadiusHeightFromAlt(lla[2]);
                    cone.setGroundRange(rh[0]);
                    cone.setHeight(rh[1]*0.990); // minus a little because of rendering artifacts
                    cone.setColor( new Color(satColor.getRed(), satColor.getGreen(), satColor.getBlue(), circleViewTransparency) );
                    cone.render(dc);
                }
            }
            
            
        } // for each sat

        
        gl.glPopAttrib();
        
     } // render
    
    private AnnotationAttributes createFontAttribs(Color textColor)
    {
        AnnotationAttributes geoAttr = new AnnotationAttributes();
            geoAttr.setFrameShape(FrameFactory.SHAPE_NONE);  // No frame
            geoAttr.setFont(Font.decode("Arial-ITALIC-12"));
            geoAttr.setTextColor(textColor);
            geoAttr.setTextAlign(MultiLineTextRenderer.ALIGN_CENTER);
            geoAttr.setDrawOffset(new Point(0, 5)); // centered just above
            geoAttr.setEffect(MultiLineTextRenderer.EFFECT_OUTLINE);  // Black outline
            geoAttr.setBackgroundColor(Color.BLACK);
            
            return geoAttr;
    } //createFontAttribs
    
    public double calcFootPrintRadiusFromAlt(double alt) // double lat, double lon, 
    {
        double earthRad = globe.getEquatorialRadius();
        double lambda0 = Math.acos(earthRad/(earthRad+alt));
        
        // length through earth from nadir point to horizon
        //double radius = earthRad*Math.sin(lambda0); 
        
        // arc length frp, nadir point to horizon edge
        //double arcLenght = lambda0/(2.0*Math.PI) * 2.0 * Math.PI * earthRad;
        double arcLenght = lambda0 * earthRad;
        
//        // projection length radius)
//        double projLenth = earthRad*Math.sin(lambda0);
        
        return arcLenght;
    }
    
    public double[] calcConeRadiusHeightFromAlt(double alt)
    {
        double[] rh = new double[2];
        
        double earthRad = globe.getEquatorialRadius();
        double lambda0 = Math.acos(earthRad/(earthRad+alt));
        
        // projection length radius)
        rh[0] = earthRad*Math.sin(lambda0);
        
        // height
        rh[1] = earthRad+alt-earthRad*Math.cos(lambda0);
        
        return rh;
    }
    
    public void updateMJD(double MJD, double eciRotDeg)
    {
        for (AbstractSatellite sat : satHash.values()) // search through all sat nodes
        {
            // set position -- should NOT DO THIS HERE! (should do this only when needs updat - when time changes
            // DIES HERE IF NO 3D MODEL  - I.E. 3D model not selected either!
            sat.getThreeDModel().setPosition(new Position(Angle.fromRadians(sat.getLatitude()),
                    Angle.fromRadians(sat.getLongitude()),
                    sat.getAltitude()));
            // set roll pitch yaw (assume user wants LVLH, velcorty aligned)
//                        sat.getThreeDModel().setYawDeg(-(90-sat.getKeplarianElements()[2]*180.0/Math.PI));
//                        //sat.getThreeDModel().setYawDeg(-(90-51.6-10)); ///90-
//                        sat.getThreeDModel().setPitchDeg(-sat.getLatitude());
//                        sat.getThreeDModel().setRollDeg(sat.getLongitude());


            // REALLY NEED MOD VELOCITY!!
            sat.getThreeDModel().setMainRotationAngleAxis(sat.getMODVelocity());
            // set velcoity for test plotting
            sat.getThreeDModel().velUnitVec = MathUtils.UnitVector(sat.getMODVelocity());
        //Vec4 vec = dc.getGlobe().computePointFromPosition(sat.getThreeDModel().getPosition());
        //System.out.println("MOD Pos:" + vec.x +"," + vec.y +"," +vec.z);

        //System.out.println("unit V:" + sat.getThreeDModel().velUnitVec[0] +"," + sat.getThreeDModel().velUnitVec[1] +"," +sat.getThreeDModel().velUnitVec[2]);
            
            // Set ECI angle
            sat.getThreeDModel().setEciRotAngleDeg(eciRotDeg);
        }
         
         
    } // update MJD
    
}
