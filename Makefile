OUTPUT_PATH_PT=outputs_pt
OUTPUT_PATH_TLB=outputs_tlb

clean:
	rm config_[1-6]_v.txt config_[1-6]_vv.txt

classes:
	javac -cp src -d bin src/cpu/CPU.java src/os/OS.java src/simulator/*.java

configs_pt: config1_pt config2_pt config3_pt config4_pt config5_pt config6_pt

config1_pt: classes
	java -cp bin simulator.Simulator -v config_1 > config_1_v.txt
	diff $(OUTPUT_PATH_PT)/config_1_v.txt config_1_v.txt

config2_pt: classes
	java -cp bin simulator.Simulator -v config_2 > config_2_v.txt
	diff $(OUTPUT_PATH_PT)/config_2_v.txt config_2_v.txt

config3_pt: classes
	java -cp bin simulator.Simulator -v config_3 > config_3_v.txt
	diff $(OUTPUT_PATH_PT)/config_3_v.txt config_3_v.txt

config4_pt: classes
	java -cp bin simulator.Simulator -v config_4 > config_4_v.txt
	diff $(OUTPUT_PATH_PT)/config_4_v.txt config_4_v.txt

config5_pt: classes
	java -cp bin simulator.Simulator -v config_5 > config_5_v.txt
	diff $(OUTPUT_PATH_PT)/config_5_v.txt config_5_v.txt

config6_pt: classes
	java -cp bin simulator.Simulator -v config_6 > config_6_v.txt
	diff $(OUTPUT_PATH_PT)/config_6_v.txt config_6_v.txt

configs_tlb: config1_tlb config2_tlb config3_tlb config4_tlb config5_tlb config6_tlb

config1_tlb: classes
	java -cp bin simulator.Simulator -v -t config_1 > config_1_v.txt
	diff $(OUTPUT_PATH_TLB)/config_1_v.txt config_1_v.txt

config2_tlb: classes
	java -cp bin simulator.Simulator -v -t config_2 > config_2_v.txt
	diff $(OUTPUT_PATH_TLB)/config_2_v.txt config_2_v.txt

config3_tlb: classes
	java -cp bin simulator.Simulator -v -t config_3 > config_3_v.txt
	diff $(OUTPUT_PATH_TLB)/config_3_v.txt config_3_v.txt

config4_tlb: classes
	java -cp bin simulator.Simulator -v -t config_4 > config_4_v.txt
	diff $(OUTPUT_PATH_TLB)/config_4_v.txt config_4_v.txt

config5_tlb: classes
	java -cp bin simulator.Simulator -v -t config_5 > config_5_v.txt
	diff $(OUTPUT_PATH_TLB)/config_5_v.txt config_5_v.txt

config6_tlb: classes
	java -cp bin simulator.Simulator -v -t config_6 > config_6_v.txt
	diff $(OUTPUT_PATH_TLB)/config_6_v.txt config_6_v.txt


turnin_setup:
	tar -cvf proj4_`whoami`.tar.gz README src/os/OS.java src/cpu/CPU.java

turnin: turnin_setup
	turnin --submit yjkwon proj4_rockhold proj4_`whoami`.tar.gz

