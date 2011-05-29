min
obj: s_ij

st

\ b_ix + b_iy = 1
\ b_jx + b_jy = 1
b_ix = 1
b_jx = 0

g + h = 1

temp_ijx - b_ix + b_jx >= 0
temp_ijx - b_jx + b_ix >= 0


g = 1 -> temp_ijx - b_ix + b_jx <= 0
h = 1 -> temp_ijx - b_jx + b_ix <= 0

s_ij - temp_ijx = 0


bin
b_ix b_jx s_ij
temp_ijx temp_jix1
g h
\ temp_ijx temp_ijy temp_jix temp_jiy
