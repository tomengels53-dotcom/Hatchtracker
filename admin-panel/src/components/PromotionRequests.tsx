import React, { useState, useEffect } from 'react';
import { getFirestore, collection, query, onSnapshot, orderBy } from 'firebase/firestore';
import { getFunctions, httpsCallable } from 'firebase/functions';

const PromotionRequests: React.FC = () => {
    const [requests, setRequests] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const db = getFirestore();
    const functions = getFunctions();

    useEffect(() => {
        const q = query(collection(db, 'traitPromotionRequests'), orderBy('lastUpdatedAt', 'desc'));
        const unsubscribe = onSnapshot(q, (snapshot) => {
            const reqList = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            setRequests(reqList);
            setLoading(false);
        });
        return () => unsubscribe();
    }, [db]);

    const handleApprove = async (requestId: string) => {
        if (!window.confirm("Approve this trait for the official Breed Standard?")) return;

        const approve = httpsCallable(functions, 'adminApproveTraitPromotion');
        try {
            await approve({ requestId });
            alert("Trait promoted successfully.");
        } catch (err: any) {
            alert(`Error: ${err.message}`);
        }
    };

    const handleReject = async (requestId: string) => {
        const reason = window.prompt("Reason for rejection:");
        if (reason === null) return;

        const reject = httpsCallable(functions, 'adminRejectTraitPromotion');
        try {
            await reject({ requestId, adminNotes: reason });
            alert("Request rejected.");
        } catch (err: any) {
            alert(`Error: ${err.message}`);
        }
    };

    if (loading) return <div>Loading requests...</div>;

    return (
        <div className="admin-table-container">
            <h2>Trait Promotion Requests</h2>
            <table className="admin-table">
                <thead>
                    <tr>
                        <th>Breed</th>
                        <th>Trait</th>
                        <th>Evidence Count</th>
                        <th>Confidence</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {requests.map(req => (
                        <tr key={req.id}>
                            <td>{req.breedId}</td>
                            <td><code>{req.traitId}</code></td>
                            <td>{req.evidenceCount}</td>
                            <td>{(req.confidenceScore * 100).toFixed(1)}%</td>
                            <td><span className={`badge status-${req.status}`}>{req.status}</span></td>
                            <td>
                                {req.status === 'pending' && (
                                    <>
                                        <button onClick={() => handleApprove(req.id)} className="btn-primary">Approve</button>
                                        <button onClick={() => handleReject(req.id)} className="btn-danger">Reject</button>
                                    </>
                                )}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default PromotionRequests;
