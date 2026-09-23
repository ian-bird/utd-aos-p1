(define (partial fn . first-args)
  (lambda rest-args
    (apply fn (append first-args rest-args))))

(define (compose f . rest-fns)
  (if (null? rest-fns)
      f
      (let ((composed (apply compose rest-fns)))
	(lambda args
	  (f (apply composed args))))))

(define (1+ x) (+ x 1))

(define complement (partial compose not))

(define (take n seq)
  (do ((i 0 (1+ i))
       (coll '() (cons (car seq) coll))
       (seq seq (cdr seq)))
      
      ((= i n) (reverse coll))))

(define (slurp path)
  (call-with-port (open-input-file path)
    (lambda (p)
      (do ((next '() (read-char p))
	   (chars (list) (cons next chars)))
	  ((eof-object? next) (list->string (cdr (reverse chars))))))))

(define range
  (case-lambda ((to) (range 0 to 1))
	       ((from to) (range from to 1))
	       ((from to by) (do ((i from (+ i by))
				  (seq '() (cons i seq)))
				 ((>= i to) (reverse seq))))))

(define (remove proc seq) (filter (complement proc) seq))

(use-modules (ice-9 match))

(define (exec)
  (match-let* ((lines (remove (partial string=? "")
			      (map (lambda (line) (car (string-split line #\#)))
				   (remove (partial string=? "")
					   (string-split (slurp "project/p1/src/main/resources/testconfig.txt")
							 #\newline)))))
	       ((num-nodes _min-send/active _max-send/active _min-send-delay _snapshot-delay _max-total-send)
		(map string->number
		     (remove (partial string=? "")
			     (string-split (car lines) #\space))))
	       (actions (map (lambda (n) (string-append "cd ~/src/utd-aos-p1/project/p1/target/classes; "
							"java utd/aos/p1/Test "
							(number->string n)
							" 2>&1 | tee ~/src/utd-aos-p1/output"
							(number->string n)
							".txt &"))
			     (range num-nodes))))
    (map system actions)))
