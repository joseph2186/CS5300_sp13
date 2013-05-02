fromnetid = 0.6642
rejectmin = 0.99 * fromnetid
rejectlimit = rejectmin + 0.01

f = open('edges.txt', 'r')
fo = open('edges-output.txt', 'w')
count = 0
#PR = float((1.0/685229.0)*(1.0-0.85))
PR = float(1.0/685230.0)
print PR
prevdata = 0
degree = 0
list_output = ''
prevdata1 = 0
flag = 0
tab = '\t'
delim = '\t'
edge_count = 0

for line in f:
        line1 = ' '.join(line.split())
        data = line1.split(' ')
        if len(data) < 3:
                print "fucked!"
        if not(((float(data[2]) >= float(rejectmin)) and (float(data[2]) < float(rejectlimit)))):
		edge_count = edge_count + 1
                if (int(data[0]) == int(prevdata)):
                       degree += 1
                       list_output += data[1]+delim
                       continue
                elif degree == 0:
                        list_output += str(prevdata1)+delim
                        degree = 1
                output = str(prevdata)+tab+str(PR)+delim+str(degree)+delim+list_output+"\n"
                #print str(output)
                fo.write(str(output))
                prevdata = data[0]
                prevdata1 = data[1]
                degree = 0
                list_output = ''
        
if degree == 0:
        output = str(data[0])+tab+str(PR)+delim+str(degree+1)+delim+str(data[1])
else:
        output = str(data[0])+tab+str(PR)+delim+str(degree)+delim+"\n"
#print str(output)
fo.write(str(output))
print "edge count is"
print edge_count

f.close()
fo.close()
