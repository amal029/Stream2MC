min
obj: l

st

\All the numbers should go to the right. Only + and - are allowed, all
\others need to be modeled with linear algebra

\Note that variables can only be initialized to literals, there can be
\no partial functions

a_a = 0

b_ap1 + b_ap2 = 1
b_bp1 + b_bp2 = 1
b_cp1 + b_cp2 = 1

s_ab - b_ap1 + b_bp1 >= 0
s_ab + b_ap1 - b_bp1 >= 0
s_ab - b_ap2 + b_bp2 >= 0
s_ab + b_ap2 - b_bp2 >= 0
s_bc - b_bp1 + b_cp1 >=0 
s_bc + b_bp1 - b_cp1 >=0 
s_bc - b_bp2 + b_cp2 >=0 
s_bc + b_bp2 - b_cp2 >=0 
s_ac + b_ap1 - b_cp1 >= 0
s_ac - b_ap1 + b_cp1 >= 0
s_ac + b_ap2 - b_cp2 >= 0
s_ac - b_ap2 + b_cp2 >= 0

b_ap1_bp1 - b_ap1 - b_bp1 >= -1
b_ap1_bp2 - b_ap1 - b_bp2 >= -1
b_ap1_cp1 - b_ap1 - b_cp1 >= -1
b_ap1_cp2 - b_ap1 - b_cp2 >= -1
b_bp1_cp2 - b_bp1 - b_cp2 >= -1
b_bp1_cp1 - b_bp1 - b_cp1 >= -1
b_ap2_bp1 - b_ap1 - b_bp1 >= -1
b_ap2_bp2 - b_ap2 - b_bp2 >= -1
b_ap2_cp1 - b_ap2 - b_cp1 >= -1
b_ap2_cp2 - b_ap2 - b_cp2 >= -1
b_bp2_cp2 - b_bp2 - b_cp2 >= -1
b_bp2_cp1 - b_bp2 - b_cp1 >= -1

r_ab + r_ba + s_ab = 1
r_ac + r_ca + s_ac = 1
r_cb + r_bc + s_bc = 1

a_b - a_a - 3 b_ap1 - 6 b_ap2 - 12345678 r_ab >= - 12345678
a_a - a_b - 4 b_bp1 - 2 b_bp2 - 12345678 r_ba >= - 12345678
a_c - a_a - 3 b_ap1 - 6 b_ap2 - 12345678 r_ac >= - 12345678
a_a - a_c - 4 b_cp1 - 4 b_cp2 - 12345678 r_ca >= - 12345678
a_c - a_b - 4 b_bp1 - 2 b_bp2 - 12345678 r_bc >= - 12345678
a_b - a_c - 4 b_cp1 - 4 b_cp2 - 12345678 r_cb >= - 12345678


a_b - a_a - 3 b_ap1 - 6 b_ap2 - 1 b_ap1_bp1 - 1 b_ap1_bp2 - 1 b_ap2_bp1 - 1 b_ap2_bp2 >= 0
a_c - a_b - 4 b_bp1 - 2 b_bp2 - 1 b_bp1_cp1 - 1 b_bp1_cp2 - 1 b_bp2_cp1 - 1 b_bp2_cp2 >= 0

l - a_c - 4 b_cp1 - 4 b_cp2 >= 0


bin
b_ap1 b_ap2 b_bp1 b_bp2 b_cp1 b_cp2 
r_ab r_ba r_ac r_ca r_bc r_cb s_ab s_ac s_bc

b_ap1_bp1 b_ap1_bp2 b_ap1_cp1 b_ap1_cp2
b_bp1_cp2 b_bp1_cp1
b_ap2_bp1 b_ap2_bp2 b_ap2_cp1 b_ap2_cp2
b_bp2_cp2 b_bp2_cp1

end
