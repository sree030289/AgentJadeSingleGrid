package org.microgrid.jade;

import jade.core.Agent;

public class MainContainer {


        public static void main(String [] args)
        {
            String [] args1 ={"-gui"};


            jade.Boot.main(args1);

            String [] args2 ={"-container","Battery:org.microgrid.jade.Battery;SolarGen:org.microgrid.jade.SolarGenerator;WindGen:org.microgrid.jade.WindGenerator;Load:org.microgrid.jade.Load"};

            //String [] args2 ={"-container","Load:org.microgrid.jade.Load"};

            jade.Boot.main(args2);




        }


    }


