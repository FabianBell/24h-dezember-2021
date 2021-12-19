.PHONY: all gfpgan arcane

all: gfpgan arcane

gfpgan:
	cd GFPGAN; docker build -t restore_service .

arcane:
	cd ArcaneGAN; docker build -t arcane_service .
	
