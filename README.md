# INSPIRE

INSPIRE is a simulation software for testing mechanical ventilators through the creation of a digital twin for the respiratory system. It comes with a Swing interface so that it can be used more easily, but it is also available as a web-app. 

#### Table of contents
* [Project Overview](#project-overview)
* [Installation (Swing interface)](#installation-swing-interface)
* [Installation (web-app)](#installation-web-app)
* [How to build a custom model with YAML](#how-to-build-a-custom-model-with-yaml)
* [Swing interface usage](#swing-interface-usage)
* [Web-app usage](#web-app-usage)
* [Troubleshooting](#troubleshooting)

## Project overview

The project is composed of four main components:
* a circuit simulator which is able to solve electrical circuits 
* a lung simulator which manages the chosen model for the respiratory system and the simulation business logic
* a ventilator simulator which simulates the ventilator that has to be tested
* a graphic user interface

## Installation (Swing interface)
In order to use the Swing interface that comes with this project, the following folders have to be downloaded: circuit-simulator-master, lungsimulator-lib, lungsimulator-swing and zeromq_schema_ventilation. Java 1.8 is required.

To start the simulation process, run manually the main method in both Main and Ventilator classes, located respectively in lungsimulator-swing and zeromq_schema_ventilation projects.

## Installation (web-app)
In order to start the web-app, the following folders have to be downloaded: circuit-simulator-master, lungsimulator-lib, zeromq_schema_ventilation and web-app. 
Java 11 is also required.

First of all, run command `mvn install` for circuit-simulator-master and lungsimulator-lib and then run command `mvn jetty:run` for the web-app project.
If all commands succeeded, the web-app can be started from your browser at `localhost:8080`, otherwise check [Troubleshooting](#troubleshooting) section for help.

Finally, run manually the main class of zeromq_schema_ventilation folder.

## How to build a custom model with YAML
This project has been conceived to allow the user to create and test its own customized model. In order to avoid exceptions and get a better understanding on how the model should be built, a detailed guide is presented. 

A custom model is composed of three different YAML files:
* the circuit model file where all the circuit components are listed
* the archetype file with all the required intial or constant values of each circuit component
* the demographic patient data file where some basic patient's info are provided

#### The circuit model file
The circuit model file has two main fields: a schema number (`schema`) and a circuit components list (`elementsList`). The first one is an arbitrary number which is used to check that both circuit model file and archetype file are associated to the same circuit. Hence, this number must be equal in both files. The latter contains all the elements of the chosen circuit model. Each element is composed of the following fields:
* `elementName`: the name or id of the given element
* `associatedFormula`: a description of the formula that has to be used to compute the element value
* `type`: type of element (resistor, capacitor, etc...)
* `x`, `y`, `x1`, `y1`: coordinates of the element nodes
* `showLeft`, `idLeft`, `showRight`, `idRight`: optional parameters to set if the left node or the right node (or both) should be displayed as key pressure points

For instance, a constant resistance description is shown in the following code. The fields `isTimeDependent` and `isExternal` will be both set to false because if the element is constant, it won't have a time dependency and its value has to be reported in the archetype file.  
```yaml
schema: 2
elementsList:
- elementName: Endo-tracheal Tube Resistance
  associatedFormula:
    isTimeDependent: false
    isExternal: false
    formula: resistance1
    variables:
    - resistance1
  type: ResistorElm
  x: 0
  y: 1
  x1: 1 
  y1: 1
```
Instead, if an element is time dependent a proper description would look like the following snippet of code. The field `formula` shows how the value should be computed while in the field `variables` are listed all the formula's parameters. Please note that the `TIME` variable must be written in capital letters and other formats won't be accepted and will result in exceptions. Finally, a key pressure point is associated to the right node.
```yaml
schema: 5
elementsList:
- elementName: Upper Airway Resistance 
  associatedFormula:
    isTimeDependent: true
    isExternal: false
    formula: resistanceU + sin(TIME)
    variables:
    - resistanceU
    - TIME
  type: ResistorElm
  x: 0
  y: 0
  x1: 1 
  y1: 0
  showRight: true
  idRight: Alveoli
```
The last example shows how the ventilator should be included in the circuit model. Since its value is given through an asynchronous call performed by ZMQ, this element won't have a formula and the field `isExternal` is set to true. External elements are indeed elements whose value does not appear in the archetype file.
```yaml
schema: 6
elementsList:
- elementName: Ventilator 
  associatedFormula:
    isTimeDependent: false
    isExternal: true
  type: ExternalVoltageElm
  x: 1
  y: 0
  x1: 0 
  y1: 2
```
For a complete file example, please refer to the default [models](https://github.com/foselab/mvm-adapt/tree/simulatore-paziente-ventilatore/lungsimulator-lib/resources/resourcereader) (lung-model-modelName.yaml) included in this project.

#### The archetype file
The archetype file has two fields: a schema number (`schema`) and a variables list (`parameters`). The first one is an arbitrary number which is used to check that both circuit model file and archetype file are associated to the same circuit. Hence, this number must be equal in both files. The latter contains all the variables values but time listed in each element field `variables` of the chosen circuit model.

For instance, a circuit model with all constant resistors and capacitors would look like the following snippet.
```yaml
schema: 2
parameters:
   resistance1: 8.0
   resistance2: 20.0
   capacitor1: 0.020
   capacitor2: 0.15
```
For other file examples, please refer to the default [models](https://github.com/foselab/mvm-adapt/tree/simulatore-paziente-ventilatore/lungsimulator-lib/resources/resourcereader) (archetype-modelName.yaml) included in this project.

#### The demographic patient data file
The last file which has to be provided for a complete model is the patient demographic data file. At the moment, this file does not influence the outcome of the simulation but it is still required for completeness.

The following snippet of code shows the demographic data of a 75 years-old female patient that is high 1.9 m and weighs 100 kg.
```yaml
gender: female
age: 75
height: 1.9
weight: 100
```

## Swing interface usage
After launching the application, a dialog window will automatically pop up as shown in the figure below. The first step is the choice of the model from the drop-down box. There are two main options available: choose a default model among those proposed or select "Your own model..." option to upload a custom model.

![Select Model View](https://github.com/foselab/inspire/blob/main/readme-files/SI_SelectModelView.png)

As soon as a model is picked a new window will appear on the screen and the simulation will start immediately after. The simulation view is composed by the following blocks:
* on the top left the circuit elements list is displayed 
* below the circuit elements, there are two buttons: a start/stop button where the user can control the simulation and a print button which save the current plots in an Images folder located in lungsimulator project
* on the bottom left a list of demographic patient data is reported
* on the top right the flow plot is displayed and can be changed by selecting an available option from the drop-down box above it
* on the bottom right the pressure plot is displayed and can be changed by selecting an available option from the drop-down box above it

![Simulation View](https://github.com/foselab/inspire/blob/main/readme-files/SI_SimulationView.png)

To begin a new simulation, the window must be closed and the project re-started.

## Web-app usage
After the browser page has loaded, a dialog window will automatically pop up as shown in the figure below. The first step is the choice of the model from the drop-down box. There are two main options available: choose a default model among those proposed or select "Your own model..." option to upload a custom model.

![Select Model View](https://github.com/foselab/inspire/blob/main/readme-files/WA_SelectModelView.png)

As soon as a model is picked a new window will appear on the screen. The simulation view is composed by the following blocks:
* on the top left the circuit elements list is displayed 
* below the circuit elements, there are two buttons to launch and stop the simulation 
* on the bottom left a list of demographic patient data is reported
* on the top right the flow plot is displayed and can be changed by selecting an available option from the drop-down box above it
* on the bottom right the pressure plot is displayed and can be changed by selecting an available option from the drop-down box above it

![Simulation View](https://github.com/foselab/inspire/blob/main/readme-files/WA_SimulationViewStop.png)

To start the simulation, the start button has to be clicked. During the simulation circuit elements values can be changed as well as the flow and pressure plot shown. To save a plot image, pick an option from the three-line botton on the top right of each plot.

To begin a new simulation with a different model, the project has to be re-started.

## Troubleshooting

