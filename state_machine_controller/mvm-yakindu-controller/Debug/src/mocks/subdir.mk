################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../src/mocks/BreathingMonitor.cpp \
../src/mocks/Serial.cpp \
../src/mocks/ValvesController.cpp 

OBJS += \
./src/mocks/BreathingMonitor.o \
./src/mocks/Serial.o \
./src/mocks/ValvesController.o 

CPP_DEPS += \
./src/mocks/BreathingMonitor.d \
./src/mocks/Serial.d \
./src/mocks/ValvesController.d 


# Each subdirectory must supply rules for building sources it contributes
src/mocks/%.o: ../src/mocks/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: Cygwin C++ Compiler'
	g++ -I"F:\Repository MVM\mvm-adapt\state_machine_controller\mvm-yakindu-controller\src" -I"F:\Repository MVM\mvm-adapt\state_machine_controller\mvm-yakindu-controller\src\src-gen" -I"F:\Repository MVM\mvm-adapt\state_machine_controller\mvm-yakindu-controller\src\mocks" -I"F:\Repository MVM\mvm-adapt\state_machine_controller\mvm-yakindu-controller\libzmq\include" -I"F:\Repository MVM\mvm-adapt\state_machine_controller\mvm-yakindu-controller\libzmq" -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


